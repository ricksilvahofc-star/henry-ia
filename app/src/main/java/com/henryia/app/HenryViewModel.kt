package com.henryia.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.henryia.app.ai.AiRouter
import com.henryia.app.ai.OpenRouterProvider
import com.henryia.app.ai.LocalTools
import com.henryia.app.ai.WebLookup
import com.henryia.app.core.ApiKeyStore
import com.henryia.app.core.ConversationStore
import com.henryia.app.core.MemoryStore
import com.henryia.app.core.model.Attachment
import com.henryia.app.core.model.ChatConversation
import com.henryia.app.core.model.ChatMessage
import com.henryia.app.core.model.MessageRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HenryViewModel(app: Application) : AndroidViewModel(app) {
    private val keyStore = ApiKeyStore(app)
    private val router = AiRouter(listOf(OpenRouterProvider { keyStore.getOpenRouterKey() }))
    private val webLookup = WebLookup()
    private val store = ConversationStore(app)
    private val memoryStore = MemoryStore(app)

    private val _conversations = MutableStateFlow(store.load())
    val conversations: StateFlow<List<ChatConversation>> = _conversations.asStateFlow()

    private val _activeId = MutableStateFlow(_conversations.value.firstOrNull()?.id ?: 1L)
    val activeId: StateFlow<Long> = _activeId.asStateFlow()

    private val _messages = MutableStateFlow(activeConversation()?.messages ?: initialMessages())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var nextId = _messages.value.maxOfOrNull { it.id }?.plus(1) ?: 2L
    private var nextConversationId = _conversations.value.maxOfOrNull { it.id }?.plus(1) ?: 1L

    init {
        if (_conversations.value.isEmpty()) {
            createConversation()
        }
    }

    fun hasApiKey(): Boolean = keyStore.getOpenRouterKey().isNotBlank()
    fun saveApiKey(key: String) = keyStore.setOpenRouterKey(key)
    fun memoryCount(): Int = memoryStore.load().size\n    fun clearMemory() = memoryStore.clear()

    fun newChat() {
        createConversation()
    }

    fun selectConversation(id: Long) {
        if (_isGenerating.value) return
        val conversation = _conversations.value.firstOrNull { it.id == id } ?: return
        _activeId.value = id
        _messages.value = conversation.messages
        nextId = (_messages.value.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    fun send(text: String, forceWeb: Boolean = false, attachments: List<Attachment> = emptyList()) {
        val clean = text.trim()
        if (clean.isEmpty() || _isGenerating.value) return

        val attachmentNote = if (attachments.isEmpty()) "" else "\n\n📎 " + attachments.joinToString(", ") { it.name }
        val userMessage = ChatMessage(nextId++, MessageRole.USER, clean + attachmentNote)
        _messages.value = _messages.value + userMessage
        updateActive(title = titleFor(clean))
        
        viewModelScope.launch {
            _isGenerating.value = true
            try {
                LocalTools.tryCalculate(clean)?.let { result ->
                    _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, result)
                    updateActive()
                    return@launch
                }
                rememberIfRequested(clean)
                val context = _messages.value.dropLast(1).takeLast(10).map { it.text }
                val webResult = if (forceWeb || isWebRequest(clean)) webLookup.search(clean) else ""

                val answer = if (!hasApiKey() && webResult.isNotBlank() && attachments.isEmpty()) {
                    "Pesquisei na web e encontrei isto:\n\n$webResult"
                } else {
                    val prompt = if (webResult.isNotBlank()) {
                        """
                        Responda à pergunta do usuário usando os dados abaixo como contexto de uma consulta web.
                        Diferencie fatos encontrados na web de informações que você não consegue confirmar.
                        Se os dados não forem suficientes, diga isso claramente.

                        Pergunta do usuário:
                        $clean

                        Dados da consulta web:
                        $webResult
                        """.trimIndent()
                    } else clean
                    router.generate(prompt, context, attachments)
                }

                _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, answer)
                updateActive()
            } finally {
                _isGenerating.value = false
            }
        }
    }

    private fun createConversation() {
        val id = nextConversationId++
        val conversation = ChatConversation(id, "Nova conversa", initialMessages())
        _conversations.value = listOf(conversation) + _conversations.value
        _activeId.value = id
        _messages.value = conversation.messages
        nextId = 2L
        store.save(_conversations.value)
    }

    private fun updateActive(title: String? = null) {
        val updated = _conversations.value.map { conversation ->
            if (conversation.id == _activeId.value) {
                conversation.copy(
                    title = title ?: conversation.title,
                    messages = _messages.value
                )
            } else conversation
        }
        _conversations.value = updated
        store.save(updated)
    }

    private fun activeConversation(): ChatConversation? =
        _conversations.value.firstOrNull { it.id == _activeId.value }

    private fun titleFor(text: String): String =
        text.replace(Regex("\\s+"), " ").trim().take(32).ifBlank { "Nova conversa" }

    private fun initialMessages(): List<ChatMessage> =
        listOf(ChatMessage(1L, MessageRole.HENRY, "Olá! Eu sou o Henry. Como posso ajudar?"))

    private fun rememberIfRequested(text: String) {
        val lower = text.lowercase()
        if (lower.startsWith("lembre que ") || lower.startsWith("lembre:") || lower.startsWith("memorize ")) {
            val fact = text.substringAfter(" ", "").trim().removePrefix("que ").removePrefix(":").trim()
            if (fact.isNotBlank()) memoryStore.add(fact)
        }
    }

    private fun isWebRequest(text: String): Boolean {
        val query = text.lowercase()
        val triggers = listOf(
            "pesquise", "pesquisar", "pesquisa", "procure", "procurar",
            "busque", "buscar", "internet", "na web", "web", "notícias",
            "noticia", "atualizado", "atualizada", "hoje", "agora"
        )
        return triggers.any { query.contains(it) }
    }
}
