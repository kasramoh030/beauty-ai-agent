package com.aipn.connect.ui

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipn.connect.App
import com.aipn.connect.PreferenceStore
import com.aipn.connect.ThemeChoice
import com.aipn.connect.data.KeyVault
import com.aipn.connect.data.Provider
import com.aipn.connect.data.Providers
import com.aipn.connect.data.Session
import com.aipn.connect.data.SessionStore
import com.aipn.connect.data.StoredMessage
import com.aipn.connect.net.ChatEngine
import com.aipn.connect.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

data class UiMessage(
    val id: Long,
    val role: String,          // "user" | "assistant"
    val text: String,
    val imageMime: String? = null,
    val imageData: String? = null,
    val providerName: String? = null,
    val model: String? = null,
    val pending: Boolean = false,
    val error: String? = null,
)

/** One provider row on the keys page. */
data class KeyCardState(
    val provider: Provider,
    val key: String = "",
    val customModels: String = "",
    val model: String = "",
    val hasKey: Boolean = false,
    val enabled: Boolean = true,
    val testing: Boolean = false,
    val testResult: String? = null,
    val availableModels: List<String> = emptyList(),
    val requests: Int = 0,
) {
    /** Hand-written models first, then whatever the provider reported, then the defaults. */
    val models: List<String>
        get() {
            val custom = customModels.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            return (custom + availableModels + provider.models).distinct()
        }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    val vault: KeyVault = app.keyVault
    val prefs = PreferenceStore(application)
    private val engine = ChatEngine(vault)
    private val sessionStore = SessionStore(application)

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()

    private val _cards = MutableStateFlow(Providers.ALL.map { KeyCardState(it) })
    val cards = _cards.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<Session>>(emptyList())
    val chatHistory = _chatHistory.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings = _settings.asStateFlow()

    private var sendJob: Job? = null
    private var sessionId: String = UUID.randomUUID().toString().take(8)

    data class SettingsState(
        val theme: ThemeChoice = ThemeChoice.SYSTEM,
        val languageTag: String = "",
        val systemPrompt: String = "",
        val temperature: Float = 0.7f,
        val maxTokens: Int = 2048,
        val streaming: Boolean = true,
        val autoFallback: Boolean = true,
        val totalRequests: Int = 0,
    )

    init {
        reloadSettings()
        refreshCards()
        _chatHistory.value = sessionStore.load()
        openSession(_chatHistory.value.firstOrNull()?.id)
    }

    private fun reloadSettings() {
        _settings.value = SettingsState(
            theme = prefs.theme,
            languageTag = prefs.languageTag,
            systemPrompt = prefs.systemPrompt,
            temperature = prefs.temperature,
            maxTokens = prefs.maxTokens,
            streaming = prefs.streaming,
            autoFallback = prefs.autoFallback,
            totalRequests = vault.totalRequests(),
        )
    }

    fun refreshCards() {
        val custom = vault.customModels()
        _cards.value = Providers.ALL.map { provider ->
            KeyCardState(
                provider = provider,
                key = vault.apiKey(provider.id) ?: "",
                customModels = custom[provider.id] ?: "",
                model = vault.modelFor(provider.id),
                hasKey = vault.hasKey(provider.id),
                enabled = vault.isEnabled(provider.id),
                requests = vault.requestCount(provider.id),
            )
        }
    }

    // ---- settings -----------------------------------------------------------

    fun setTheme(theme: ThemeChoice) { prefs.theme = theme; reloadSettings() }
    fun setLanguage(tag: String) { prefs.languageTag = tag; reloadSettings() }
    fun setSystemPrompt(value: String) { prefs.systemPrompt = value; reloadSettings() }
    fun setTemperature(value: Float) { prefs.temperature = value; reloadSettings() }
    fun setMaxTokens(value: Int) { prefs.maxTokens = value.coerceIn(128, 32768); reloadSettings() }
    fun setStreaming(value: Boolean) { prefs.streaming = value; reloadSettings() }
    fun setAutoFallback(value: Boolean) { prefs.autoFallback = value; reloadSettings() }
    fun resetStats() { vault.resetStats(); reloadSettings(); refreshCards() }

    // ---- keys ---------------------------------------------------------------

    /** Local text of the key field, committed only when the user saves. */
    fun setDraftKey(providerId: String, value: String) =
        _cards.value = _cards.value.map { if (it.provider.id == providerId) it.copy(key = value) else it }

    fun setDraftModels(providerId: String, value: String) =
        _cards.value = _cards.value.map { if (it.provider.id == providerId) it.copy(customModels = value) else it }

    fun saveKey(providerId: String) {
        val card = _cards.value.firstOrNull { it.provider.id == providerId } ?: return
        vault.setApiKey(providerId, card.key.trim())
        vault.setCustomModels(providerId, card.customModels)
        refreshCards()
    }

    fun removeKey(providerId: String) {
        vault.clearApiKey(providerId)
        refreshCards()
    }

    fun setEnabled(providerId: String, enabled: Boolean) {
        vault.setEnabled(providerId, enabled)
        refreshCards()
    }

    fun setModel(providerId: String, model: String) {
        vault.setModelFor(providerId, model)
        refreshCards()
    }

    fun currentModel(): String {
        val provider = candidates().firstOrNull() ?: return Providers.ALL.first().defaultModel
        return vault.resolvedModel(provider)
    }

    fun testKey(providerId: String) {
        val provider = Providers.byId(providerId) ?: return
        _cards.value = _cards.value.map {
            if (it.provider.id == providerId) it.copy(testing = true, testResult = null) else it
        }
        viewModelScope.launch {
            val result = engine.listModels(provider)
            _cards.value = _cards.value.map {
                if (it.provider.id == providerId) {
                    it.copy(
                        testing = false,
                        testResult = if (result.ok) "${result.models.size} models" else result.error,
                        availableModels = if (result.ok) result.models else it.availableModels,
                    )
                } else it
            }
        }
    }

    fun loadModels(providerId: String) {
        val provider = Providers.byId(providerId) ?: return
        viewModelScope.launch {
            val result = engine.listModels(provider)
            _cards.value = _cards.value.map {
                if (it.provider.id == providerId) {
                    it.copy(
                        availableModels = result.models.ifEmpty { it.availableModels },
                        testResult = if (result.ok) "${result.models.size} models" else it.testResult,
                    )
                } else it
            }
        }
    }

    // ---- chat history -------------------------------------------------------

    fun newChat() {
        stop()
        sessionId = UUID.randomUUID().toString().take(8)
        _messages.value = emptyList()
    }

    fun openSession(id: String?) {
        val session = _chatHistory.value.firstOrNull { it.id == id } ?: return
        stop()
        sessionId = session.id
        _messages.value = session.messages.mapIndexed { index, m ->
            UiMessage(
                id = System.nanoTime() + index,
                role = m.role,
                text = m.text,
                providerName = m.provider,
                model = m.model,
                error = m.error,
            )
        }
    }

    fun activeSessionId(): String? =
        if (_messages.value.isEmpty()) null else sessionId

    fun deleteSession(id: String) {
        val remaining = _chatHistory.value.filterNot { it.id == id }
        sessionStore.save(remaining)
        _chatHistory.value = remaining
        if (id == sessionId) newChat()
    }

    fun deleteAllSessions() {
        sessionStore.clear()
        _chatHistory.value = emptyList()
        newChat()
    }

    private fun persist() {
        val stored = _messages.value
            .filter { it.text.isNotBlank() || it.error != null }
            .map { StoredMessage(it.role, it.text, it.providerName, it.model, it.error) }
        if (stored.isEmpty()) return
        val title = stored.first { it.role == "user" }.text.take(40)
        val updated = (_chatHistory.value.filterNot { it.id == sessionId } +
            Session(sessionId, title, System.currentTimeMillis(), stored))
            .sortedByDescending { it.createdAt }
        sessionStore.save(updated)
        _chatHistory.value = updated
    }

    // ---- chat ---------------------------------------------------------------

    private fun candidates(): List<Provider> {
        val enabled = engine.fallbackOrder(vault.enabledIds())
        return if (prefs.autoFallback) enabled else enabled.take(1)
    }

    fun activeModel(): Pair<String, String>? {
        val provider = candidates().firstOrNull() ?: return null
        return provider.name to vault.resolvedModel(provider)
    }

    fun suggestedProvider(): Provider? = candidates().firstOrNull() ?: Providers.byId("gemini")

    fun send(text: String, image: Pair<String, String>? = null) {
        if (text.isBlank() && image == null) return
        if (_sending.value) return

        val userMessage = UiMessage(
            id = System.nanoTime(),
            role = "user",
            text = text.trim(),
            imageMime = image?.first,
            imageData = image?.second,
        )
        val pendingId = System.nanoTime() + 1
        _messages.value = _messages.value + userMessage +
            UiMessage(id = pendingId, role = "assistant", text = "", pending = true)

        _sending.value = true
        val history = _messages.value
            .filter { it.id != pendingId && it.text.isNotBlank() }
            .map { it.role to it.text }
        val providerList = candidates()

        sendJob = viewModelScope.launch {
            try {
                engine.streamChat(
                    candidates = providerList,
                    model = if (providerList.isNotEmpty()) vault.resolvedModel(providerList.first()) else "",
                    history = history,
                    systemPrompt = prefs.systemPrompt,
                    temperature = prefs.temperature,
                    maxTokens = prefs.maxTokens,
                    streaming = prefs.streaming,
                    image = image,
                ).collect { turn ->
                    _messages.value = _messages.value.map { message ->
                        if (message.id == pendingId) {
                            message.copy(
                                text = turn.text,
                                pending = false,
                                providerName = turn.provider.name,
                                model = turn.model,
                                error = null,
                            )
                        } else message
                    }
                }
            } catch (t: Throwable) {
                _messages.value = _messages.value.map { message ->
                    if (message.id == pendingId) {
                        message.copy(text = "", pending = false, error = Http.explain(t))
                    } else message
                }
            } finally {
                _sending.value = false
                persist()
                reloadSettings()
                refreshCards()
            }
        }
    }

    fun stop() {
        sendJob?.cancel()
        sendJob = null
        _sending.value = false
    }

    fun regenerate() {
        val user = _messages.value.lastOrNull { it.role == "user" } ?: return
        val image = user.imageMime?.let { mime -> user.imageData?.let { data -> mime to data } }
        val index = _messages.value.indexOf(user)
        _messages.value = _messages.value.take(index)
        send(user.text, image)
    }

    fun clearChat() {
        stop()
        _messages.value = emptyList()
    }

    fun deleteMessage(id: Long) {
        _messages.value = _messages.value.filterNot { it.id == id }
        persist()
    }

    // ---- image attachment ---------------------------------------------------

    suspend fun prepareImage(uri: Uri): Pair<String, String>? = withContext(Dispatchers.IO) {
        try {
            val resolver = app.contentResolver
            val boundsWidth = resolver.openInputStream(uri)?.use { input ->
                val options = android.graphics.BitmapFactory.Options()
                options.inJustDecodeBounds = true
                android.graphics.BitmapFactory.decodeStream(input, null, options)
                options.outWidth
            } ?: return@withContext null

            val bitmap = resolver.openInputStream(uri)?.use { input ->
                val options = android.graphics.BitmapFactory.Options()
                options.inSampleSize = sampleSizeFor(boundsWidth, 1280)
                android.graphics.BitmapFactory.decodeStream(input, null, options)
            } ?: return@withContext null

            val output = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 82, output)
            bitmap.recycle()
            "image/jpeg" to Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (t: Throwable) {
            android.util.Log.w("AppViewModel", "image load failed", t)
            null
        }
    }

    private fun sampleSizeFor(size: Int, max: Int): Int {
        var sample = 1
        var value = size
        while (value / 2 >= max) {
            value /= 2
            sample *= 2
        }
        return sample
    }
}

/** Lets screens read the active language without threading it through every call. */
object LocaleState {
    var isPersian: Boolean = false
}