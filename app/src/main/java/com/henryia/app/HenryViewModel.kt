package com.henryia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.henryia.app.ai.AiRouter
import com.henryia.app.core.model.ChatMessage
import com.henryia.app.core.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HenryViewModel : ViewModel() {
    private val router = AiRouter()
    private var nextId = 2L

    private val _messages = MutableStateFlow(
        listOf(ChatMessage(1L, MessageRole.HENRY, "Olá! Eu sou o Henry. Como posso ajudar?"))
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    fun newChat() {
        nextId = 2L
        _messages.value = listOf(
            ChatMessage(1L, MessageRole.HENRY, "Nova conversa iniciada. O que vamos fazer?")
        )
    }

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return

        val userId = nextId++
        _messages.value = _messages.value + ChatMessage(userId, MessageRole.USER, clean)

        viewModelScope.launch {
            val answer = router.generate(clean, _messages.value.takeLast(12).map { it.text })
            _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, answer)
        }
    }
}
