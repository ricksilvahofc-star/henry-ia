package com.henryia.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.henryia.app.ai.AiRouter
import com.henryia.app.ai.OpenRouterProvider
import com.henryia.app.ai.WebLookup
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
    private val webLookup = WebLookup()
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

    fun send(text: String, forceWeb: Boolean = false) {
        val clean = text.trim()
        if (clean.isEmpty() || _isGenerating.value) return

        val userMessage = ChatMessage(nextId++, MessageRole.USER, clean)
        _messages.value = _messages.value + userMessage
        persist()

        viewModelScope.launch {
            _isGenerating.value = true
            try {
                val context = _messages.value.dropLast(1).takeLast(10).map { it.text }
                val webResult = if (forceWeb || isWebRequest(clean)) webLookup.search(clean) else ""

                val answer = if (!hasApiKey() && webResult.isNotBlank()) {
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
                    } else {
                        clean
                    }
                    router.generate(prompt, context)
                }

                _messages.value = _messages.value + ChatMessage(nextId++, MessageRole.HENRY, answer)
                persist()
            } finally {
                _isGenerating.value = false
            }
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
