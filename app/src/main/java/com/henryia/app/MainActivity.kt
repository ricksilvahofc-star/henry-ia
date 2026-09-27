package com.henryia.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class Message(
    val text: String,
    val fromUser: Boolean
)

class MainActivity : ComponentActivity() {

    private val preferences by lazy {
        getSharedPreferences("henry_settings", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var input by remember { mutableStateOf("") }
            var isSending by remember { mutableStateOf(false) }
            var showKeyDialog by remember { mutableStateOf(false) }
            var apiKey by remember {
                mutableStateOf(preferences.getString("gemini_api_key", "") ?: "")
            }

            val messages = remember {
                mutableStateListOf(
                    Message(
                        "Olá! Eu sou o Henry IA. Agora já posso conversar com você usando inteligência artificial.",
                        false
                    )
                )
            }

            fun sendMessage() {
                val text = input.trim()
                if (text.isBlank() || isSending) return

                if (apiKey.isBlank()) {
                    showKeyDialog = true
                    return
                }

                messages.add(Message(text, true))
                input = ""
                isSending = true

                lifecycleScope.launch {
                    val history = messages.toList()
                    val result = GeminiClient.generateResponse(apiKey, history)

                    messages.add(
                        Message(
                            result.getOrElse {
                                "Não consegui responder agora. Verifique sua chave da API e sua conexão com a internet."
                            },
                            false
                        )
                    )
                    isSending = false
                }
            }

            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Henry IA",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                                Text(
                                    text = "Seu assistente pessoal",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            TextButton(onClick = { showKeyDialog = true }) {
                                Text("Chave")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { message ->
                                Text(
                                    text = if (message.fromUser) {
                                        "Você: ${message.text}"
                                    } else {
                                        "Henry: ${message.text}"
                                    },
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = input,
                                onValueChange = { input = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Fale com o Henry...") },
                                singleLine = true,
                                enabled = !isSending
                            )

                            Button(
                                onClick = { sendMessage() },
                                enabled = !isSending && input.isNotBlank()
                            ) {
                                Text(if (isSending) "..." else "Enviar")
                            }
                        }
                    }
                }
            }

            if (showKeyDialog) {
                AlertDialog(
                    onDismissRequest = { showKeyDialog = false },
                    title = { Text("Chave da API Gemini") },
                    text = {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Cole sua chave aqui") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                preferences.edit()
                                    .putString("gemini_api_key", apiKey.trim())
                                    .apply()
                                apiKey = apiKey.trim()
                                showKeyDialog = false
                            }
                        ) {
                            Text("Salvar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showKeyDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }
        }
    }
}

interface AiProvider {
    val name: String

    suspend fun generateResponse(
        apiKey: String,
        messages: List<Message>
    ): Result<String>
}

object AiRouter {
    private val gemini = GeminiProvider

    fun chooseProvider(message: String): AiProvider {
        // Futuramente o Henry poderá escolher automaticamente entre
        // Gemini, outros modelos, pesquisa, código, imagem e ferramentas.
        return gemini
    }
}

object GeminiProvider : AiProvider {
    override val name = "Gemini"

    private const val MODEL = "gemini-2.5-flash-lite"
    private const val ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"

    override suspend fun generateResponse(
        apiKey: String,
        messages: List<Message>
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val url = URL(ENDPOINT)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey)
            }

            val contents = JSONArray()

            messages.forEach { message ->
                if (message.text.isNotBlank()) {
                    contents.put(
                        JSONObject()
                            .put("role", if (message.fromUser) "user" else "model")
                            .put(
                                "parts",
                                JSONArray().put(JSONObject().put("text", message.text))
                            )
                    )
                }
            }

            val body = JSONObject()
                .put(
                    "systemInstruction",
                    JSONObject().put(
                        "parts",
                        JSONArray().put(
                            JSONObject().put(
                                "text",
                                "Você é Henry, um assistente pessoal em português do Brasil. " +
                                    "Seja útil, natural, direto e amigável. " +
                                    "Não diga que é humano. Quando não souber algo, seja transparente."
                            )
                        )
                    )
                )
                .put("contents", contents)
                .put(
                    "generationConfig",
                    JSONObject().put("maxOutputTokens", 1024)
                )

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val status = connection.responseCode
            val responseText = (if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }).bufferedReader().use { it.readText() }

            connection.disconnect()

            if (status !in 200..299) {
                throw IllegalStateException("API Gemini retornou HTTP $status")
            }

            val json = JSONObject(responseText)
            val text = json
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            text.trim()
        }
    }
}

object GeminiClient {
    suspend fun generateResponse(
        apiKey: String,
        messages: List<Message>
    ): Result<String> {
        return AiRouter.chooseProvider(
            messages.lastOrNull { it.fromUser }?.text.orEmpty()
        ).generateResponse(apiKey, messages)
    }
}
