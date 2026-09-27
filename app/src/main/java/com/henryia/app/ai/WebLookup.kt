package com.henryia.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class WebLookup {

    suspend fun search(query: String): String = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext ""

        val encoded = URLEncoder.encode(clean, "UTF-8")
        val url = URL(
            "https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1&no_redirect=1"
        )

        val connection = (url.openConnection() as HttpURLConnection)
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Henry-IA/0.1")

            val status = connection.responseCode
            if (status !in 200..299) return@withContext "A consulta à web falhou (HTTP " + status + ")."

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            val parts = mutableListOf<String>()

            val heading = json.optString("Heading")
            val abstractText = json.optString("AbstractText")
            val abstractUrl = json.optString("AbstractURL")

            if (heading.isNotBlank() && abstractText.isNotBlank()) {
                parts += "Resumo: " + heading + " — " + abstractText +
                    if (abstractUrl.isNotBlank()) " (fonte: " + abstractUrl + ")" else ""
            }

            collectTopics(json.optJSONArray("RelatedTopics"), parts)

            if (parts.isEmpty()) return@withContext "A consulta web não encontrou um resumo direto para essa pergunta."

            buildString {
                append("RESULTADOS DA CONSULTA WEB:\n")
                parts.take(6).forEachIndexed { index, item ->
                    append(index + 1)
                    append(". ")
                    append(item)
                    append("\n")
                }
            }
        } catch (_: Exception) {
            "Não consegui consultar a web agora. Verifique a conexão com a internet."
        } finally {
            connection.disconnect()
        }
    }

    private fun collectTopics(array: JSONArray?, output: MutableList<String>) {
        if (array == null) return
        for (i in 0 until array.length()) {
            if (output.size >= 6) return
            val item = array.optJSONObject(i) ?: continue
            val text = item.optString("Text")
            val firstUrl = item.optString("FirstURL")
            if (text.isNotBlank()) {
                output += text + if (firstUrl.isNotBlank()) " (fonte: " + firstUrl + ")" else ""
            }
            collectTopics(item.optJSONArray("Topics"), output)
        }
    }
}
