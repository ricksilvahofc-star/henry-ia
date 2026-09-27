package com.henryia.app.ai

data class MasterPlan(
    val title: String,
    val steps: List<String>
)

object MasterPlanner {
    fun detect(prompt: String): MasterPlan? {
        val p = prompt.trim().lowercase()
        val triggers = listOf("crie uma ia", "criar uma ia", "crie um aplicativo", "criar um aplicativo", "crie um app", "criar um app", "crie um site", "criar um site", "faça um aplicativo", "fazer um aplicativo")
        if (triggers.none { p.contains(it) }) return null
        val title = when {
            p.contains("ia") -> "Novo projeto de IA"
            p.contains("site") -> "Novo site"
            else -> "Novo aplicativo"
        }
        return MasterPlan(title, listOf(
            "Definir objetivo e requisitos",
            "Escolher arquitetura e tecnologias",
            "Criar estrutura de pastas e arquivos",
            "Implementar a primeira versão",
            "Testar e corrigir erros",
            "Gerar uma versão pronta para uso"
        ))
    }

    fun promptFor(plan: MasterPlan, original: String): String =
        "Você está no Modo Mestre e recebeu um pedido de criação de software.\n" +
        "Projeto: " + plan.title + "\n" +
        "Etapas sugeridas:\n" + plan.steps.mapIndexed { i, s -> (i + 1).toString() + ". " + s }.joinToString("\n") +
        "\n\nPedido original:\n" + original +
        "\n\nTransforme isso em um plano técnico executável. Inclua arquitetura, arquivos necessários, " +
        "tecnologias gratuitas quando possível, código inicial e próximos passos. Não finja ter " +
        "acesso a arquivos, serviços ou execução que não estejam disponíveis."
}