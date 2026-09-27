package com.henryia.app.core.model
enum class MessageRole { USER, HENRY }
data class ChatMessage(val id: Long, val role: MessageRole, val text: String)
