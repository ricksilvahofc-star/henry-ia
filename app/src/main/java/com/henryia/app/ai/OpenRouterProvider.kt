package com.henryia.app.ai

import com.henryia.app.core.model.Attachment
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterProvider(private val apiKeyProvider: () -> String) : AiProvider {
    override val id = "openrouter"
    override val displayName = "OpenRouter Free"

    override suspend fun generate(prompt: String, context: List<String>, attachments: List<Attachment>): String {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) return "Para eu responder de verdade, abra Configurações e coloque sua chave do OpenRouter."

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content",
            "Você é Henry, um assistente de IA em português do Brasil. Seja útil, claro e direto. " +
            "Você pode conversar, explicar, programar, analisar anexos e ajudar a criar projetos. " +
            "Siga as instruções de tarefa recebidas no prompt. Não invente fatos, fontes, resultados de execução ou acesso a serviços. " +
            "Quando não tiver informação suficiente, diga o que falta. Quando o usuário pedir código, entregue código completo quando isso for mais útil. " +
            "Quando houver contexto de conversa, use-o para manter continuidade."
        ))

        context.takeLast(10).forEachIndexed { index, text ->
            messages.put(JSONObject().put("role", if (index % 2 == 0) "user" else "assistant").put("content", text))
        }

        val userContent = JSONArray()
        userContent.put(JSONObject().put("type", "text").put("text", prompt))
        attachments.take(4).forEach { attachment ->
            if (attachment.mimeType.startsWith("image/")) {
                val encoded = attachment.uri.substringAfter("base64,", "")
                if (encoded.isNotBlank()) {
                    userContent.put(JSONObject().put("type", "image_url")
                        .put("image_url", JSONObject().put("url", "data:" + attachment.mimeType + ";base64," + encoded)))
                }
            } else {
                userContent.put(JSONObject().put("type", "text")
                    .put("text", "Anexo: " + attachment.name + " (" + attachment.mimeType + ")."))
            }
        }
        messages.put(JSONObject().put("role", "user").put("content", userContent))

        val body = JSONObject().put("model", "openrouter/free").put("messages", messages)
            .put("temperature", 0.7).put("max_tokens", 1600).toString()

        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer " + key)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Title", "Henry")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                return "Não consegui falar com a IA agora. Código " + status +
                    if (!detail.isNullOrBlank()) ": " + detail else "."
            }

            JSONObject(response).optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                ?.optString("content")?.takeIf { it.isNotBlank() } ?: "A IA respondeu sem texto."
        } catch (_: Exception) {
            "Não consegui conectar à IA. Verifique sua internet e tente novamente."
        } finally {
            connection.disconnect()
        }
    }
}
