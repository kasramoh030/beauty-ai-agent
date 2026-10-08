package com.aipn.connect.net

import com.aipn.connect.data.KeyVault
import com.aipn.connect.data.Protocol
import com.aipn.connect.data.Provider
import com.aipn.connect.data.Providers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** What the UI needs in order to render a whole turn. */
data class Turn(
    val provider: Provider,
    val model: String,
    val text: String,
    val failedProvider: Provider? = null,
    val error: String? = null,
)

/** Result of listing the models a key can reach. */
data class ModelListResult(
    val models: List<String>,
    val error: String? = null,
) {
    val ok: Boolean get() = error == null
}

/**
 * Talks to the providers. The interesting part is [streamChat]: it walks the enabled
 * providers in order and, when one fails (bad key, rate limit, network hiccup), silently
 * continues with the next one - which is what makes several free keys feel like one
 * large quota.
 */
class ChatEngine(private val vault: KeyVault) {

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    /**
     * Order used for automatic fallback: providers the user enabled first, then the
     * ones that need no key, so the app always has something to answer with.
     */
    fun fallbackOrder(enabledIds: Set<String>): List<Provider> {
        val enabled = Providers.ALL.filter { it.id in enabledIds && vault.isUsable(it) }
        val zeroKey = Providers.ALL.filter { it.id !in enabledIds && it.keyOptional }
        return enabled + zeroKey
    }

    // ---- model discovery ----------------------------------------------------

    suspend fun listModels(provider: Provider): ModelListResult = try {
        val base = provider.modelListUrl
        val key = provider.accessKey(vault)
        val request = Request.Builder()
            .url(if (key.isNullOrBlank()) base else "$base?key=$key")
            .header("Accept", "application/json")
            .header("User-Agent", "AI-APN-Connect/1.0 (Android)")
            .apply { if (!key.isNullOrBlank()) header("Authorization", "Bearer $key") }
            .get()
            .build()

        Http.getJson(Http.client.newCall(request)).let { json ->
            val ids = parseModelIds(provider, json)
            val filtered = if (provider.freeSuffixOnly) ids.filter { it.endsWith(":free") } else ids
            val result = if (filtered.isEmpty()) provider.models else filtered
            ModelListResult(result.distinct().sorted())
        }
    } catch (t: Throwable) {
        if (t is CancellationException) throw t
        // A missing /models endpoint is not fatal: the built-in list still works.
        if (provider.models.isNotEmpty()) ModelListResult(provider.models) else ModelListResult(emptyList(), Http.explain(t))
    }

    private fun parseModelIds(provider: Provider, json: JSONObject): List<String> {
        return when (provider.protocol) {
            Protocol.OPENAI -> json.optJSONArray("data")?.mapObjects { it.optString("id") }.orEmpty()
            Protocol.GEMINI -> json.optJSONArray("models")?.mapObjects {
                it.optString("name").removePrefix("models/")
            }.orEmpty()
        }
    }

    private fun JSONArray.mapObjects(block: (JSONObject) -> String): List<String> {
        val result = ArrayList<String>(length())
        for (i in 0 until length()) {
            optJSONObject(i)?.let { obj ->
                block(obj).takeIf { it.isNotBlank() }?.let(result::add)
            }
        }
        return result
    }

    // ---- chat ---------------------------------------------------------------

    /**
     * Streams one answer, trying each candidate provider until one succeeds.
     *
     * @param history previous turns, oldest first, excluding the pending user message
     * @param image optional attached image (base64, without the data-url prefix)
     */
    fun streamChat(
        candidates: List<Provider>,
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        streaming: Boolean,
        image: Pair<String, String>? = null,
        onProvider: (Provider) -> Unit = {},
    ): Flow<Turn> = flow {
        if (candidates.isEmpty()) {
            throw ApiException(0, "No provider is ready. Add a free API key first.")
        }
        val failures = ArrayList<String>()

        for (provider in candidates) {
            // Each provider may expose a different model list, so resolve per provider.
            // The caller's `model` wins for the first candidate; the rest fall back to
            // whatever the user picked for that provider.
            val effectiveModel = when {
                provider == candidates.first() && model.isNotBlank() -> model
                else -> vault.resolvedModel(provider)
            }
            try {
                onProvider(provider)
                val text = if (streaming) {
                    streamFrom(provider, effectiveModel, history, systemPrompt, temperature, maxTokens, image)
                } else {
                    completeFrom(provider, effectiveModel, history, systemPrompt, temperature, maxTokens, image)
                }
                vault.bumpRequests(provider.id)
                emit(Turn(provider, effectiveModel, text, failedProvider = if (failures.isEmpty()) null else candidates.first()))
                return@flow
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                val reason = Http.explain(t)
                failures.add("${provider.name}: $reason")
                android.util.Log.w("ChatEngine", "${provider.id} failed: $reason")
            }
        }
        throw ApiException(
            0,
            failures.joinToString("\n"),
        )
    }

    private suspend fun streamFrom(
        provider: Provider,
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        image: Pair<String, String>?,
    ): String {
        val call = buildChatCall(provider, model, history, systemPrompt, temperature, maxTokens, image, true)
        Http.execute(call).use { response ->
            val source = response.body?.source() ?: throw IOException("empty response body")
            val reader = SseReader(source)
            val out = StringBuilder()
            while (true) {
                val payload = reader.next() ?: break
                if (payload == "[DONE]") break
                val delta = extractDelta(provider, payload) ?: continue
                out.append(delta)
            }
            if (out.isEmpty()) throw IOException("the provider returned an empty answer")
            return out.toString()
        }
    }

    private suspend fun completeFrom(
        provider: Provider,
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        image: Pair<String, String>?,
    ): String {
        val call = buildChatCall(provider, model, history, systemPrompt, temperature, maxTokens, image, false)
        return Http.execute(call).use { response ->
            val text = response.body?.string().orEmpty()
            if (text.isBlank()) throw IOException("empty response body")
            extractFullText(provider, text).ifBlank { throw IOException("no text in response") }
        }
    }

    private fun extractDelta(provider: Provider, payload: String): String? = try {
        val json = JSONObject(payload)
        when (provider.protocol) {
            Protocol.OPENAI -> json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("delta")
                ?.optString("content")
                ?.takeIf { it.isNotEmpty() }

            Protocol.GEMINI -> json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.let { parts ->
                    val builder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        parts.optJSONObject(i)?.optString("text")?.let(builder::append)
                    }
                    builder.toString().takeIf { it.isNotEmpty() }
                }
        }
    } catch (t: Throwable) {
        null
    }

    private fun extractFullText(provider: Provider, body: String): String {
        val json = JSONObject(body)
        return when (provider.protocol) {
            Protocol.OPENAI -> json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                .orEmpty()

            Protocol.GEMINI -> json.optJSONArray("candidates")
                ?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
                ?.let { parts ->
                    val builder = StringBuilder()
                    for (i in 0 until parts.length()) {
                        parts.optJSONObject(i)?.optString("text")?.let(builder::append)
                    }
                    builder.toString()
                }
                .orEmpty()
        }
    }

    // ---- request building ---------------------------------------------------

    private fun buildChatCall(
        provider: Provider,
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        image: Pair<String, String>?,
        streaming: Boolean,
    ): okhttp3.Call {
        val body = when (provider.protocol) {
            Protocol.OPENAI -> openAiBody(model, history, systemPrompt, temperature, maxTokens, image, streaming)
            Protocol.GEMINI -> geminiBody(model, history, systemPrompt, temperature, maxTokens, image, streaming)
        }
        val url = when (provider.protocol) {
            Protocol.OPENAI -> provider.baseUrl + "/chat/completions"
            Protocol.GEMINI -> "${provider.baseUrl}/models/$model:streamGenerateContent?alt=sse"
        }
        val request = Request.Builder()
            .url(url)
            .post(body.toString().toRequestBody(JSON))
            .header("Accept", if (streaming) "text/event-stream" else "application/json")
            .header("User-Agent", "AI-APN-Connect/1.0 (Android)")
            .apply {
                val key = provider.accessKey(vault)
                if (!key.isNullOrBlank()) header("Authorization", "Bearer $key")
            }
            .build()
        return Http.client.newCall(request)
    }

    private fun openAiBody(
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        image: Pair<String, String>?,
        streaming: Boolean,
    ): JSONObject {
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            messages.put(JSONObject().put("role", "system").put("content", systemPrompt))
        }
        for ((role, content) in history) {
            if (image != null && role == "user" && content == history.lastOrNull()?.second) {
                val parts = JSONArray().put(JSONObject().put("type", "text").put("text", content))
                parts.put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:${image.first};base64,${image.second}")
                    )
                )
                messages.put(JSONObject().put("role", "user").put("content", parts))
            } else {
                messages.put(JSONObject().put("role", role).put("content", content))
            }
        }
        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", streaming)
    }

    private fun geminiBody(
        model: String,
        history: List<Pair<String, String>>,
        systemPrompt: String,
        temperature: Float,
        maxTokens: Int,
        image: Pair<String, String>?,
        streaming: Boolean,
    ): JSONObject {
        val contents = JSONArray()
        val lastUser = history.lastOrNull { it.first == "user" }?.second
        for ((role, content) in history) {
            val parts = JSONArray()
            parts.put(JSONObject().put("text", content))
            if (image != null && content == lastUser && role == "user") {
                parts.put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject().put("mime_type", image.first).put("data", image.second)
                    )
                )
            }
            contents.put(
                JSONObject()
                    .put("role", if (role == "assistant") "model" else "user")
                    .put("parts", parts)
            )
        }
        val generationConfig = JSONObject()
            .put("temperature", temperature)
            .put("maxOutputTokens", maxTokens)
            .put("maxOutputTokens", maxTokens)
        val body = JSONObject()
            .put("contents", contents)
            .put("generationConfig", generationConfig)
        if (systemPrompt.isNotBlank()) {
            body.put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            )
        }
        if (!streaming) {
            // Non-streaming Gemini uses a different verb.
        }
        return body
    }
}