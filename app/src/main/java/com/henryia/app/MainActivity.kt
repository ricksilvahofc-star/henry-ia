package com.henryia.app

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Message(
    val text: String,
    val fromUser: Boolean
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var input by remember { mutableStateOf("") }

            val messages = remember {
                mutableStateListOf(
                    Message(
                        "Olá! Eu sou o Henry IA. Minha base está pronta. Agora vamos me dar inteligência, memória, voz e ferramentas.",
                        false
                    )
                )
            }

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {

                        Text(
                            text = "Henry IA",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        Text(
                            text = "Seu assistente pessoal",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
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
                                placeholder = {
                                    Text("Fale com o Henry...")
                                },
                                singleLine = true
                            )

                            Button(
                                onClick = {
                                    if (input.isNotBlank()) {

                                        messages.add(
                                            Message(
                                                input.trim(),
                                                true
                                            )
                                        )

                                        messages.add(
                                            Message(
                                                "Recebi sua mensagem. Meu motor de IA será conectado na próxima etapa.",
                                                false
                                            )
                                        )

                                        input = ""
                                    }
                                }
                            ) {
                                Text("Enviar")
                            }
                        }
                    }
                }
            }
        }
    }
}
