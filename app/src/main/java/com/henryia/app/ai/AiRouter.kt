package com.henryia.app.ai

import com.henryia.app.core.model.Attachment

class AiRouter(private val providers: List<AiProvider> = emptyList()) {
    suspend fun generate(
        prompt: String,
        context: List<String>,
        attachments: List<Attachment> = emptyList()
    ): String = providers.firstOrNull()?.generate(prompt, context, attachments)
        ?: "O Henry está pronto. Agora vamos conectar um cérebro de IA."
}
