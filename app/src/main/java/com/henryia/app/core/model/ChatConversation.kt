package com.henryia.app.core.model

data class ChatConversation(
    val id: Long,
    val title: String,
    val messages: List<ChatMessage>
)
