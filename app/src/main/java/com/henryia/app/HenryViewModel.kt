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

class HenryViewModel(app: Application) : AndroidViewModel(app) {
    private val keyStore = ApiKeyStore(app)
    private val router = AiRouter(listOf(OpenRouterProvider { keyStore.getOpenRouterKey() }))
    private var nextId = 2L
    private val _messages = MutableStateFlow(listOf(ChatMessage(1L, MessageRole.HENRY, "Olá! Eu sou o Henry. Como posso ajudar?")))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun hasApiKey(): Boolean = keyStore.getOpenRouterKey().isNotBlank()
    fun saveApiKey(key: String) = keyStore.setOpenRouterKey(key)

    fun newChat() {
        nextId = 2L
        _messages.value = listOf(ChatMessage(1L, MessageRole.HENRY, "Nova conversa iniciada. O que vamos fazer?"))
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.USER, clean)
        viewModelScope.launch {
            val context = _messages.value.dropLast(1).takeLast(10).map { it.text }
            val answer = router.generate(clean, context)
            _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, answer)
        }
    }
}
