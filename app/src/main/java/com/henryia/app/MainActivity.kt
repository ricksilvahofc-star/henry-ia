package com.henryia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.henryia.app.core.model.MessageRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { HenryTheme { HenryApp() } }
    }
}

private val HenryBlue = Color(0xFF1687FF)
private val HenryCyan = Color(0xFF00D9FF)
private val HenryDark = Color(0xFF070A10)
private val HenryPanel = Color(0xFF10141C)
private val HenryBubble = Color(0xFF171C26)

@Composable
private fun HenryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = HenryBlue,
            secondary = HenryCyan,
            background = HenryDark,
            surface = HenryPanel
        ),
        content = content
    )
}

@Composable
private fun HenryApp(vm: HenryViewModel = viewModel()) {
    val messages by vm.messages.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }

    Row(Modifier.fillMaxSize().background(HenryDark)) {
        if (drawerOpen) {
            HenryDrawer(
                onNewChat = { vm.newChat(); drawerOpen = false },
                onClose = { drawerOpen = false }
            )
        }

        Column(Modifier.fillMaxSize().weight(1f)) {
            TopBar(onMenu = { drawerOpen = true })

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                if (messages.size == 1) {
                    item { WelcomeHeader() }
                }
                items(messages, key = { it.id }) { message ->
                    MessageBubble(message.role, message.text)
                }
            }

            Composer(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    vm.send(input)
                    input = ""
                }
            )
        }
    }
}

@Composable
private fun TopBar(onMenu: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, "Menu") }
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text("Henry", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Assistente de IA", color = Color.LightGray, fontSize = 12.sp)
        }
        Text("●", color = HenryCyan, fontSize = 18.sp)
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("H", color = HenryCyan, fontSize = 42.sp, fontWeight = FontWeight.ExtraBold)
        Text("Como posso ajudar?", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pergunte, crie, pesquise ou peça uma tarefa.",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MessageBubble(role: MessageRole, text: String) {
    val user = role == MessageRole.USER
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (user) HenryBlue else HenryBubble,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.widthIn(max = 340.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = Color.White,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = HenryPanel,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.AttachFile, "Anexar")
            }
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem para Henry...") },
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = HenryBlue
                )
            )
            IconButton(onClick = { }) {
                Icon(Icons.Default.Mic, "Voz")
            }
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank()
            ) {
                Icon(Icons.Default.Send, "Enviar", tint = HenryCyan)
            }
        }
    }
}

@Composable
private fun HenryDrawer(onNewChat: () -> Unit, onClose: () -> Unit) {
    Surface(
        color = Color(0xFF0C1017),
        modifier = Modifier.width(285.dp).fillMaxHeight()
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("HENRY", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HenryCyan)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Default.Menu, "Fechar") }
            }

            Button(
                onClick = onNewChat,
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Nova conversa")
            }

            Text("Conversas", color = Color.Gray, modifier = Modifier.padding(8.dp))
            Surface(
                color = HenryBubble,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { }
            ) {
                Text(
                    "Nova conversa",
                    modifier = Modifier.padding(14.dp),
                    maxLines = 1
                )
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().clickable { }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, null, tint = Color.Gray)
                Spacer(Modifier.width(10.dp))
                Text("Configurações", color = Color.LightGray)
            }
        }
    }
}
