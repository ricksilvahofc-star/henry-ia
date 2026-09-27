package com.henryia.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.henryia.app.ai.AiRouter
import com.henryia.app.ai.OpenRouterProvider
import com.henryia.app.core.ApiKeyStore
import com.henryia.app.core.model.ChatMessage
import com.henryia.app.core.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class HenryViewModel(app: Application) : AndroidViewModel(app) {
    private val keyStore = ApiKeyStore(app)
    private val prefs = app.getSharedPreferences("henry_chat", 0)
    private val router = AiRouter(listOf(OpenRouterProvider { keyStore.getOpenRouterKey() }))
    private var nextId = 1L

    private val _messages = MutableStateFlow(loadMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    fun hasApiKey(): Boolean = keyStore.getOpenRouterKey().isNotBlank()
    fun saveApiKey(key: String) = keyStore.setOpenRouterKey(key)

    fun newChat() {
        nextId = 2L
        _messages.value = listOf(ChatMessage(1L, MessageRole.HENRY, "Nova conversa iniciada. O que vamos fazer?"))
        persist()
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _isGenerating.value) return

        val userMessage = ChatMessage(nextId++, MessageRole.USER, clean)
        _messages.value = _messages.value + userMessage
        persist()

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = _messages.value.dropLast(1).takeLast(10).map { it.text }
                val answer = router.generate(clean, context)
                _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, answer)
                persist()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun loadMessages(): List<ChatMessage> {
        val raw = prefs.getString("messages", null) ?: return listOf(
            ChatMessage(1L, MessageRole.HENRY, "Olá! Eu sou o Henry. Como posso ajudar?")
        )
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        ChatMessage(
                            item.getLong("id"),
                            MessageRole.valueOf(item.getString("role")),
                            item.getString("text")
                        )
                    )
                }
            }.also { list -> nextId = (list.maxOfOrNull { it.id } ?: 0L) + 1L }
        }.getOrElse {
            listOf(ChatMessage(1L, MessageRole.HENRY, "Olá! Eu sou o Henry. Como posso ajudar?"))
        }
    }

    private fun persist() {
        val array = JSONArray()
        _messages.value.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("role", it.role.name)
                    .put("text", it.text)
            )
        }
        prefs.edit().putString("messages", array.toString()).apply()
    }
}
