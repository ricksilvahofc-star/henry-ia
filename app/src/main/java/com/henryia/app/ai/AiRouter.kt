package com.henryia.app.ai
class AiRouter(private val providers: List<AiProvider> = emptyList()) {
    suspend fun generate(prompt: String, context: List<String>): String =
        providers.firstOrNull()?.generate(prompt, context)
            ?: "O Henry está pronto. Agora vamos conectar o primeiro cérebro de IA."
}
