package com.henryia.app.core

import android.content.Context
import com.henryia.app.core.model.ChatConversation
import com.henryia.app.core.model.ChatMessage
import com.henryia.app.core.model.MessageRole
import org.json.JSONArray
import org.json.JSONObject

class ConversationStore(context: Context) {
    private val prefs = context.getSharedPreferences("henry_conversations", 0)

    fun load(): List<ChatConversation> {
        val raw = prefs.getString("conversations", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val messagesJson = item.optJSONArray("messages") ?: JSONArray()
                    val messages = buildList {
                        for (j in 0 until messagesJson.length()) {
                            val message = messagesJson.getJSONObject(j)
                            add(
                                ChatMessage(
                                    message.getLong("id"),
                                    MessageRole.valueOf(message.getString("role")),
                                    message.getString("text")
                                )
                            )
                        }
                    }
                    add(ChatConversation(item.getLong("id"), item.optString("title", "Nova conversa"), messages))
                }
            }
        }.getOrElse { emptyList() }
    }

    fun save(conversations: List<ChatConversation>) {
        val array = JSONArray()
        conversations.forEach { conversation ->
            val messages = JSONArray()
            conversation.messages.forEach { message ->
                messages.put(
                    JSONObject()
                        .put("id", message.id)
                        .put("role", message.role.name)
                        .put("text", message.text)
                )
            }
            array.put(
                JSONObject()
                    .put("id", conversation.id)
                    .put("title", conversation.title)
                    .put("messages", messages)
            )
        }
        prefs.edit().putString("conversations", array.toString()).apply()
    }
}
