package com.aipn.connect.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** One saved conversation. */
data class Session(
    val id: String,
    val title: String,
    val createdAt: Long,
    val messages: List<StoredMessage> = emptyList(),
) {
    val preview: String
        get() = messages.firstOrNull { it.role == "user" }?.text?.take(48) ?: title
}

/** A single message as persisted on disk. */
data class StoredMessage(
    val role: String,
    val text: String,
    val provider: String? = null,
    val model: String? = null,
    val error: String? = null,
)

/**
 * Conversation history kept as one small JSON file in the app's private storage.
 * Keys are never written here - only the text of what you said and what came back.
 */
class SessionStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, "sessions.json")

    fun load(): List<Session> {
        if (!file.exists()) return emptyList()
        return try {
            val array = JSONArray(file.readText())
            val list = ArrayList<Session>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val messages = ArrayList<StoredMessage>()
                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                for (j in 0 until msgArray.length()) {
                    val m = msgArray.optJSONObject(j) ?: continue
                    messages.add(
                        StoredMessage(
                            role = m.optString("role", "user"),
                            text = m.optString("text"),
                            provider = m.optString("provider").ifBlank { null },
                            model = m.optString("model").ifBlank { null },
                            error = m.optString("error").ifBlank { null },
                        )
                    )
                }
                list.add(
                    Session(
                        id = obj.optString("id"),
                        title = obj.optString("title", "گفتگو"),
                        createdAt = obj.optLong("createdAt", 0L),
                        messages = messages,
                    )
                )
            }
            list.sortedByDescending { it.createdAt }
        } catch (t: Throwable) {
            emptyList()
        }
    }

    fun save(sessions: List<Session>) {
        try {
            val array = JSONArray()
            for (session in sessions.take(60)) {
                val obj = JSONObject()
                    .put("id", session.id)
                    .put("title", session.title)
                    .put("createdAt", session.createdAt)
                val msgs = JSONArray()
                for (m in session.messages) {
                    msgs.put(
                        JSONObject()
                            .put("role", m.role)
                            .put("text", m.text)
                            .put("provider", m.provider ?: "")
                            .put("model", m.model ?: "")
                            .put("error", m.error ?: "")
                    )
                }
                obj.put("messages", msgs)
                array.put(obj)
            }
            file.writeText(array.toString())
        } catch (t: Throwable) {
            // history is a convenience; never break the chat over it
        }
    }

    fun clear() {
        try {
            if (file.exists()) file.delete()
        } catch (t: Throwable) {
        }
    }
}