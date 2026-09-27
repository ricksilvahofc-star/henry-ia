package com.henryia.app.ai

import com.henryia.app.core.model.Attachment

interface AiProvider {
    val id: String
    val displayName: String
    suspend fun generate(prompt: String, context: List<String>, attachments: List<Attachment> = emptyList()): String
}
