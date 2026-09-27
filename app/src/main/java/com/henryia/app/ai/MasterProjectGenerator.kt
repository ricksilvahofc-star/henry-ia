package com.henryia.app.ai

import org.json.JSONArray
import org.json.JSONObject

data class GeneratedProjectFile(
    val path: String,
    val content: String
)

data class GeneratedProject(
    val name: String,
    val type: ProjectType,
    val description: String,
    val files: List<GeneratedProjectFile>
)

class MasterProjectGenerator(private val router: AiRouter) {

    suspend fun generate(plan: MasterPlan, original: String, context: List<String>): GeneratedProject? {
        val request = buildPrompt(plan, original)
        val raw = router.generate(request, context)
        return parse(raw)
    }

    private fun buildPrompt(plan: MasterPlan, original: String): String {
        return """
Você está no Modo Mestre do Henry.
Crie uma primeira versão pequena, funcional e coerente do projeto solicitado.

Responda SOMENTE com JSON válido, sem markdown e sem texto antes ou depois:
{
  "name": "nome-do-projeto",
  "type": "PROJECT_TYPE",
  "description": "descrição curta",
  "files": [
    {"path": "README.md", "content": "..."},
    {"path": "arquivo.ext", "content": "..."}
  ]
}

Regras:
- Gere no máximo 5 arquivos na primeira versão.
- Todo caminho deve ser relativo ao projeto.
- O conteúdo precisa ser texto completo e utilizável.
- Não inclua senhas, chaves de API ou segredos.
- Use tecnologias gratuitas quando possível.
- Se o projeto for Android, prefira Kotlin + Jetpack Compose.
- Se for site, prefira HTML + CSS + JavaScript simples.
- Se for IA, use uma arquitetura simples e deixe credenciais em variáveis de ambiente.
- Não invente execução, testes executados ou acesso a serviços.
- O projeto deve corresponder ao pedido original.

Tipo planejado: PROJECT_TYPE
Pedido original: ORIGINAL_REQUEST
""".trimIndent()
            .replace("PROJECT_TYPE", plan.type.name)
            .replace("ORIGINAL_REQUEST", original)
    }

    private fun parse(raw: String): GeneratedProject? {
        val jsonText = extractJson(raw) ?: return null
        return runCatching {
            val json = JSONObject(jsonText)
            val filesJson = json.optJSONArray("files") ?: JSONArray()
            val files = buildList {
                for (i in 0 until filesJson.length()) {
                    val item = filesJson.optJSONObject(i) ?: continue
                    val path = item.optString("path").trim()
                    val content = item.optString("content")
                    if (path.isNotBlank()) add(GeneratedProjectFile(path, content))
                }
            }
            if (files.isEmpty()) return@runCatching null
            GeneratedProject(
                name = json.optString("name").ifBlank { "Projeto Henry" },
                type = runCatching { ProjectType.valueOf(json.optString("type")) }.getOrDefault(ProjectType.UNKNOWN),
                description = json.optString("description"),
                files = files
            )
        }.getOrNull()
    }

    private fun extractJson(raw: String): String? {
        val clean = raw.trim()
        if (clean.startsWith("{") && clean.endsWith("}")) return clean
        val start = clean.indexOf("{")
        val end = clean.lastIndexOf("}")
        return if (start >= 0 && end > start) clean.substring(start, end + 1) else null
    }
}
