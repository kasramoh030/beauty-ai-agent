package com.aipn.connect.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Raised when a provider refuses the request; carries the HTTP status for the fallback logic. */
class ApiException(val code: Int, message: String) : IOException(message)

/** Server-sent-event reader that turns a streaming HTTP response into text deltas. */
class SseReader(private val body: okio.BufferedSource) {

    /** Yields the `data:` payload of every event, decoding as UTF-8. Returns null at EOF. */
    fun next(): String? {
        val builder = StringBuilder()
        while (true) {
            val line = body.readUtf8Line() ?: return if (builder.isEmpty()) null else builder.toString()
            when {
                // blank line == end of one event
                line.isEmpty() -> if (builder.isNotEmpty()) return builder.toString()
                line.startsWith("data:") -> {
                    if (builder.isNotEmpty()) builder.append('\n')
                    builder.append(line.removePrefix("data:").trimStart())
                }
                // ignore event:, id:, retry: and comment lines
            }
        }
    }
}

object Http {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun request(url: String, apiKey: String?): Request.Builder {
        val builder = Request.Builder().url(url)
        if (!apiKey.isNullOrBlank()) builder.header("Authorization", "Bearer $apiKey")
        return builder
    }

    /** Executes a request off the main thread and verifies the response status. */
    suspend fun execute(call: Call): Response = withContext(Dispatchers.IO) {
        val response = call.execute()
        if (!response.isSuccessful) {
            val errorBody = try {
                response.body?.string()?.take(600).orEmpty()
            } catch (t: Throwable) {
                ""
            }
            response.close()
            throw ApiException(response.code, errorBody.ifBlank { "HTTP ${response.code}" })
        }
        response
    }

    /** Reads a whole JSON response, closes the connection and returns the parsed object. */
    suspend fun getJson(call: Call): JSONObject {
        val response = execute(call)
        val text = try {
            response.body?.string().orEmpty()
        } finally {
            response.close()
        }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    /** Pulls a readable message out of whatever error shape the provider returned. */
    fun explain(exception: Throwable): String = when (exception) {
        is ApiException -> {
            val detail = try {
                val json = JSONObject(exception.message ?: "")
                json.optJSONObject("error")?.optString("message")
                    ?: json.optString("message")
            } catch (t: Throwable) {
                null
            }
            val status = when (exception.code) {
                401, 403 -> "unauthorized"
                429 -> "rate-limited"
                else -> "http-${exception.code}"
            }
            detail?.takeIf { it.isNotBlank() } ?: "HTTP ${exception.code} ($status)"
        }
        is IOException -> exception.message ?: "connection failed"
        else -> exception.message ?: exception.javaClass.simpleName
    }
}