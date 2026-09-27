package com.henryia.app.ai
interface AiProvider {
    val id: String
    val displayName: String
    suspend fun generate(prompt: String, context: List<String>): String
}
