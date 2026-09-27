package com.henryia.app.ai

enum class ProjectType { AI, APP, SITE, UNKNOWN }

data class MasterFile(val path: String, val purpose: String)

data class MasterPlan(
    val title: String,
    val type: ProjectType,
    val objective: String,
    val steps: List<String>,
    val files: List<MasterFile>,
    val stack: List<String>
)

object MasterPlanner {
    fun detect(prompt: String): MasterPlan? {
        val p = prompt.trim().lowercase()
        val type = when {
            listOf("crie uma ia", "criar uma ia", "criar inteligência artificial", "criar inteligencia artificial").any { p.contains(it) } -> ProjectType.AI
            listOf("crie um aplicativo", "criar um aplicativo", "crie um app", "criar um app", "faça um aplicativo", "fazer um aplicativo").any { p.contains(it) } -> ProjectType.APP
            listOf("crie um site", "criar um site", "faça um site", "fazer um site").any { p.contains(it) } -> ProjectType.SITE
            else -> return null
        }
        val title = when (type) {
            ProjectType.AI -> "Novo projeto de IA"
            ProjectType.APP -> "Novo aplicativo"
            ProjectType.SITE -> "Novo site"
            ProjectType.UNKNOWN -> "Novo projeto"
        }
        val files = when (type) {
            ProjectType.AI -> listOf(
                MasterFile("README.md", "Objetivo, configuração e instruções"),
                MasterFile("src/core/ai", "Núcleo e regras da IA"),
                MasterFile("src/core/tools", "Ferramentas da IA"),
                MasterFile("src/main", "Ponto de entrada"),
                MasterFile(".env.example", "Variáveis sem segredos")
            )
            ProjectType.APP -> listOf(
                MasterFile("README.md", "Objetivo e instruções"),
                MasterFile("app", "Interface e lógica"),
                MasterFile("core", "Modelos, serviços e regras"),
                MasterFile("tests", "Testes da primeira versão")
            )
            ProjectType.SITE -> listOf(
                MasterFile("README.md", "Objetivo e instruções"),
                MasterFile("index.html", "Estrutura principal"),
                MasterFile("src", "Interface, estilos e lógica"),
                MasterFile("tests", "Testes da primeira versão")
            )
            ProjectType.UNKNOWN -> emptyList()
        }
        val stack = when (type) {
            ProjectType.AI -> listOf("API de modelo compatível", "Kotlin ou Python", "JSON/HTTP")
            ProjectType.APP -> listOf("Android", "Kotlin", "Jetpack Compose")
            ProjectType.SITE -> listOf("HTML", "CSS", "JavaScript")
            ProjectType.UNKNOWN -> emptyList()
        }
        return MasterPlan(
            title,
            type,
            "Transformar o pedido em uma primeira versão funcional.",
            listOf(
                "Interpretar objetivo e requisitos",
                "Definir arquitetura e tecnologia",
                "Montar estrutura de arquivos",
                "Implementar a primeira versão funcional",
                "Validar e corrigir problemas",
                "Preparar a entrega"
            ),
            files,
            stack
        )
    }

    fun promptFor(plan: MasterPlan, original: String): String {
        val filesText = plan.files.joinToString("\n") { "- " + it.path + ": " + it.purpose }
        val stackText = plan.stack.joinToString(", ")
        return "Você está no Modo Mestre do Henry e deve agir como arquiteto e engenheiro de software.\n" +
            "Projeto: " + plan.title + "\nTipo: " + plan.type.name + "\nObjetivo: " + plan.objective +
            "\nTecnologias sugeridas: " + stackText +
            "\n\nPlano de execução:\n" +
            plan.steps.mapIndexed { i, s -> (i + 1).toString() + ". " + s }.joinToString("\n") +
            "\n\nEstrutura inicial sugerida:\n" + filesText +
            "\n\nPedido original:\n" + original +
            "\n\nEntregue uma especificação executável para a primeira versão. Inclua requisitos, arquitetura, " +
            "estrutura de arquivos, contratos entre componentes, código inicial essencial e testes. " +
            "Se faltar informação, escolha uma opção simples e gratuita quando possível e deixe a suposição explícita. " +
            "Nunca invente acesso a arquivos, GitHub, serviços ou execução."
    }
}
