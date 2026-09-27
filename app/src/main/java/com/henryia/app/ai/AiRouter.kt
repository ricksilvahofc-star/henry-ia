package com.henryia.app.ai

import com.henryia.app.core.model.Attachment

enum class AiTask { CHAT, CODING, RESEARCH, CREATION }

class AiRouter(private val providers: List<AiProvider> = emptyList()) {
    fun detectTask(prompt: String): AiTask {
        val p = prompt.lowercase()
        return when {
            listOf("crie uma ia", "criar uma ia", "crie um aplicativo", "criar um aplicativo", "crie um app", "criar um app", "crie um site", "criar um site", "construa", "desenvolva").any { p.contains(it) } -> AiTask.CREATION
            listOf("código", "codigo", "programa", "programar", "kotlin", "java", "python", "javascript", "android", "bug", "erro no código", "erro no codigo").any { p.contains(it) } -> AiTask.CODING
            listOf("pesquise", "pesquisar", "procure", "buscar", "busque", "notícia", "noticia", "hoje", "agora", "atualizado", "internet", "web").any { p.contains(it) } -> AiTask.RESEARCH
            else -> AiTask.CHAT
        }
    }

    private fun taskInstruction(task: AiTask): String = when (task) {
        AiTask.CHAT -> "Responda naturalmente, de forma clara e útil."
        AiTask.CODING -> "Priorize código correto e executável. Explique mudanças importantes e, quando possível, entregue código completo."
        AiTask.RESEARCH -> "Separe informações confirmadas de incertezas. Não invente fontes ou fatos."
        AiTask.CREATION -> "Transforme o pedido em uma solução concreta. Estruture requisitos, arquitetura, arquivos e implementação de forma prática."
    }

    suspend fun generate(prompt: String, context: List<String>, attachments: List<Attachment> = emptyList()): String {
        val provider = providers.firstOrNull()
            ?: return "O Henry está pronto, mas nenhum provedor de IA foi configurado."
        val task = detectTask(prompt)
        val enrichedPrompt = "[Tipo de tarefa: " + task.name + "]\n" + taskInstruction(task) + "\n\n" + prompt
        return provider.generate(enrichedPrompt, context, attachments)
    }
}
