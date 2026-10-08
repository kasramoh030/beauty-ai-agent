package com.aipn.connect.ui

import android.app.Application
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipn.connect.PreferenceStore
import com.aipn.connect.ThemeChoice
import com.aipn.connect.data.KeyVault
import com.aipn.connect.data.Provider
import com.aipn.connect.data.Providers
import com.aipn.connect.net.ChatEngine
import com.aipn.connect.net.Http
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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
    val fellBackFrom: String? = null,
)

data class KeyStatus(
    val provider: Provider,
    val hasKey: Boolean,
    val enabled: Boolean,
    val model: String,
    val testing: Boolean = false,
    val result: String? = null,
    val models: List<String> = emptyList(),
    val requests: Int = 0,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as App
    val vault: KeyVault = app.keyVault
    val prefs = PreferenceStore(application)
    private val engine = ChatEngine(vault)

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages = _messages.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending = _sending.asStateFlow()

    private val _keys = MutableStateFlow(List(Providers.ALL) { KeyStatus(it, vault.hasKey(it.id), vault.isEnabled(it.id), vault.modelFor(it.id), requests = vault.requestCount(it.id)) })
    val keys = _keys.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings = _settings.asStateFlow()

    private var sendJob: Job? = null

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
        refreshKeys()
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

    fun refreshKeys() {
        _keys.value = Providers.ALL.map { provider ->
            KeyStatus(
                provider = provider,
                hasKey = vault.hasKey(provider.id),
                enabled = vault.isEnabled(provider.id),
                model = vault.modelFor(provider.id),
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

    fun resetStats() { vault.resetStats(); reloadSettings(); refreshKeys() }

    // ---- keys ---------------------------------------------------------------

    fun setKey(providerId: String, value: String) {
        if (value.isBlank()) vault.clearApiKey(providerId) else vault.setApiKey(providerId, value.trim())
        refreshKeys()
    }

    fun setAccountId(providerId: String, value: String) {
        vault.setAccountId(providerId, value)
        refreshKeys()
    }

    fun setEnabled(providerId: String, enabled: Boolean) {
        vault.setEnabled(providerId, enabled)
        refreshKeys()
    }

    fun setModel(providerId: String, model: String) {
        vault.setModelFor(providerId, model)
        refreshKeys()
    }

    fun testKey(providerId: String) {
        val provider = Providers.byId(providerId) ?: return
        setStatus(providerId) { copy(testing = true, result = null) }
        viewModelScope.launch {
            val result = engine.listModels(provider)
            val label = if (result.ok) "${result.models.size} models" else Http.explain(IllegalStateException(result.error ?: "failed"))
            vault.setStatus(providerId, if (result.ok) "ok" else "error", result.models.size)
            setStatus(providerId) {
                copy(testing = false, result = label, models = if (result.ok) result.models else models)
            }
        }
    }

    fun loadModels(providerId: String) {
        val provider = Providers.byId(providerId) ?: return
        viewModelScope.launch {
            val result = engine.listModels(provider)
            setStatus(providerId) {
                if (result.ok) copy(models = result.models, result = "${result.models.size} models")
                else copy(result = result.error)
            }
        }
    }

    private fun setStatus(providerId: String, block: KeyStatus.() -> KeyStatus) {
        _keys.value = _keys.value.map { if (it.provider.id == providerId) it.block() else it }
    }

    // ---- chat ---------------------------------------------------------------

    /** Providers the router may use, in fallback order. */
    private fun candidates(): List<Provider> {
        val enabled = engine.fallbackOrder(vault.enabledIds())
        return if (prefs.autoFallback) enabled else enabled.take(1)
    }

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
        val placeholder = UiMessage(id = pendingId, role = "assistant", text = "", pending = true)
        _messages.value = _messages.value + userMessage + placeholder

        _sending.value = true
        val history = _messages.value
            .filter { it.id != pendingId && it.text.isNotBlank() }
            .map { it.role to it.text }

        val providerList = candidates()
        var failures = 0

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
                    failures = turn.failedProvider?.let { failures + 1 } ?: failures
                    _messages.value = _messages.value.map { message ->
                        if (message.id == pendingId) {
                            message.copy(
                                text = turn.text,
                                pending = false,
                                providerName = turn.provider.name,
                                model = turn.model,
                                error = null,
                                fellBackFrom = if (turn.failedProvider != null) turn.failedProvider.name else null,
                            )
                        } else message
                    }
                }
            } catch (t: Throwable) {
                _messages.value = _messages.value.map { message ->
                    if (message.id == pendingId) {
                        message.copy(
                            text = "",
                            pending = false,
                            error = Http.explain(t),
                        )
                    } else message
                }
            } finally {
                _sending.value = false
                reloadSettings()
                refreshKeys()
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
        // drop the assistant turn and the user message, then send the same prompt again
        val index = _messages.value.indexOf(user)
        _messages.value = _messages.value.take(index)
        send(user.text, image)
    }

    fun clearChat() { stop(); _messages.value = emptyList() }

    fun deleteMessage(id: Long) {
        _messages.value = _messages.value.filterNot { it.id == id }
    }

    // ---- image attachment ---------------------------------------------------

    /** Loads an image, downsizes it and returns mime + base64, or null when it fails. */
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