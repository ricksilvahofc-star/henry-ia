package com.henryia.app.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenRouterProvider(private val apiKeyProvider: () -> String) : AiProvider {
    override val id = "openrouter"
    override val displayName = "OpenRouter Free"

    override suspend fun generate(prompt: String, context: List<String>): String {
        val key = apiKeyProvider().trim()
        if (key.isEmpty()) return "Para eu responder de verdade, abra Configurações e coloque sua chave do OpenRouter."

        val messages = JSONArray()
        messages.put(JSONObject().put("role", "system").put("content",
            "Você é Henry, um assistente de IA útil, direto, educado e em português do Brasil. Ajude com explicações, criação, programação e tarefas. Não invente fatos quando não tiver certeza."
        ))
        context.takeLast(10).forEachIndexed { index, text ->
            messages.put(JSONObject().put("role", if (index % 2 == 0) "user" else "assistant").put("content", text))
        }
        messages.put(JSONObject().put("role", "user").put("content", prompt))

        val body = JSONObject().put("model", "openrouter/free").put("messages", messages)
            .put("temperature", 0.7).put("max_tokens", 1200).toString()

        val connection = (URL("https://openrouter.ai/api/v1/chat/completions").openConnection() as HttpURLConnection)
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 60_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $key")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Title", "Henry")
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (status !in 200..299) {
                val detail = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull()
                return "Não consegui falar com a IA agora. Código $status" + if (!detail.isNullOrBlank()) ": $detail" else "."
            }

            JSONObject(response).optJSONArray("choices")
                ?.optJSONObject(0)?.optJSONObject("message")?.optString("content")
                ?.takeIf { it.isNotBlank() } ?: "A IA respondeu sem texto."
        } catch (e: Exception) {
            "Não consegui conectar à IA. Verifique sua internet e tente novamente."
        } finally {
            connection.disconnect()
        }
    }
}
