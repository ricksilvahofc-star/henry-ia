package com.henryia.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.net.Uri
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.content.ContextCompat
import androidx.compose.ui.graphics.Color
import com.henryia.app.core.model.MessageRole
import com.henryia.app.core.VoiceController
import com.henryia.app.core.HenrySpeaker
import com.henryia.app.core.model.Attachment
import java.io.ByteArrayOutputStream

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
    val isGenerating by vm.isGenerating.collectAsState()
    val conversations by vm.conversations.collectAsState()
    var drawerOpen by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var settingsOpen by remember { mutableStateOf(false) }
    var webEnabled by remember { mutableStateOf(false) }
    var listening by remember { mutableStateOf(false) }\n    var attachments by remember { mutableStateOf<List<Attachment>>(emptyList()) }
    val context = LocalContext.current
    val voiceController = remember { VoiceController(context) }
    val speaker = remember { HenrySpeaker(context) }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->\n        uri?.let { prepareAttachment(context, it)?.let { item -> attachments = (attachments + item).takeLast(4) } }\n    }\n    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            listening = true
            voiceController.start(
                onResult = { spoken -> input = spoken; listening = false },
                onError = { listening = false }
            )
        }
    }
    DisposableEffect(Unit) {
        onDispose { voiceController.destroy(); speaker.destroy() }
    }
    fun startVoice() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listening = true
            voiceController.start(
                onResult = { spoken -> input = spoken; listening = false },
                onError = { listening = false }
            )
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Row(Modifier.fillMaxSize().background(HenryDark)) {
        if (drawerOpen) {
            HenryDrawer(
                conversations = conversations,
                activeId = vm.activeId.collectAsState().value,
                onSelect = { vm.selectConversation(it); drawerOpen = false },
                onNewChat = { vm.newChat(); drawerOpen = false },
                onClose = { drawerOpen = false },
                onSettings = { drawerOpen = false; settingsOpen = true }
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
                    MessageBubble(message.role, message.text, onSpeak = if (message.role == MessageRole.HENRY) ({ speaker.speak(message.text) }) else null)
                }
                if (isGenerating) { item { ThinkingBubble() } }
            }

            Composer(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    vm.send(input, forceWeb = webEnabled, attachments = attachments)
                    input = ""
                    attachments = emptyList()
                },
                webEnabled = webEnabled,
                onWebToggle = { webEnabled = !webEnabled },
                onVoice = { startVoice() },
                onAttach = { fileLauncher.launch("*/*") },
                attachments = attachments,
                onRemoveAttachment = { item -> attachments = attachments.filterNot { it.uri == item.uri } },
                listening = listening,
                enabled = !isGenerating
            )
        }
        if (settingsOpen) {
            SettingsDialog(
                currentKey = if (vm.hasApiKey()) "saved" else "",
                memoryCount = vm.memoryCount(),\n                onClearMemory = { vm.clearMemory() },
                onSave = { vm.saveApiKey(it); settingsOpen = false },
                onDismiss = { settingsOpen = false }
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
        Image(painterResource(com.henryia.app.R.drawable.henry_logo), contentDescription = "Logo do Henry", modifier = Modifier.size(82.dp))
        Text("Como posso ajudar?", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(
            "Pergunte, crie, pesquise ou peça uma tarefa.",
            color = Color.Gray,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun MessageBubble(role: MessageRole, text: String, onSpeak: (() -> Unit)? = null) {
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
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 15.sp
                )
                if (onSpeak != null) {
                    TextButton(onClick = onSpeak, contentPadding = PaddingValues(0.dp)) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Ouvir resposta")
                        Spacer(Modifier.width(4.dp))
                        Text("Ouvir")
                    }
                }
            }
        }
    }
}

@Composable
private fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    webEnabled: Boolean = false,
    onWebToggle: () -> Unit = {},
    onVoice: () -> Unit = {},
    onAttach: () -> Unit = {},
    attachments: List<Attachment> = emptyList(),
    onRemoveAttachment: (Attachment) -> Unit = {},
    listening: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        color = HenryPanel,
        modifier = Modifier.fillMaxWidth().padding(12.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            if (attachments.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    attachments.take(3).forEach { item ->
                        Surface(color = HenryBlue.copy(alpha = 0.18f), shape = RoundedCornerShape(8.dp)) {
                            Row(Modifier.padding(start = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name.take(14), color = HenryCyan, fontSize = 10.sp, maxLines = 1)
                                IconButton(onClick = { onRemoveAttachment(item) }, modifier = Modifier.size(22.dp)) {
                                    Icon(Icons.Default.Close, "Remover", modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onWebToggle, enabled = enabled) {
                    Icon(
                        if (webEnabled) Icons.Default.Language else Icons.Default.AutoAwesome,
                        if (webEnabled) "Pesquisa web ativada" else "Ferramentas"
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(if (webEnabled) "Pesquisar na web com Henry..." else "Mensagem para Henry...") },
                    maxLines = 5,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = HenryBlue
                    )
                )
                if (webEnabled) {
                    Surface(color = HenryBlue.copy(alpha = 0.18f), shape = RoundedCornerShape(10.dp)) {
                        Text("WEB", color = HenryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                    }
                }
                IconButton(onClick = onAttach, enabled = enabled) {
                    Icon(Icons.Default.AttachFile, "Anexar arquivo")
                }
                IconButton(onClick = onVoice, enabled = enabled) {
                    Icon(Icons.Default.Mic, "Voz", tint = if (listening) HenryCyan else LocalContentColor.current)
                }
                IconButton(onClick = onSend, enabled = (value.isNotBlank() || attachments.isNotEmpty()) && enabled) {
                    Icon(Icons.Default.Send, "Enviar", tint = HenryCyan)
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble() {
    Row(Modifier.fillMaxWidth()) {
        Surface(color = HenryBubble, shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = HenryCyan)
                Spacer(Modifier.width(10.dp))
                Text("Henry está pensando...", color = Color.LightGray, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun HenryDrawer(conversations: List<com.henryia.app.core.model.ChatConversation>, activeId: Long, onSelect: (Long) -> Unit, onNewChat: () -> Unit, onClose: () -> Unit, onSettings: () -> Unit) {
    Surface(
        color = Color(0xFF0C1017),
        modifier = Modifier.width(285.dp).fillMaxHeight()
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Image(painterResource(com.henryia.app.R.drawable.henry_logo), contentDescription = null, modifier = Modifier.size(42.dp)); Spacer(Modifier.width(8.dp)); Text("HENRY", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = HenryCyan) }
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
            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(conversations, key = { it.id }) { conversation ->
                    Surface(
                        color = if (conversation.id == activeId) HenryBubble else Color.Transparent,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(conversation.id) }
                    ) {
                        Text(
                            conversation.title,
                            modifier = Modifier.padding(14.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Row(
                Modifier.fillMaxWidth().clickable { onSettings() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Settings, null, tint = Color.Gray)
                Spacer(Modifier.width(10.dp))
                Text("Configurações", color = Color.LightGray)
            }
        }
    }
}


@Composable
private fun SettingsDialog(
    currentKey: String,
    memoryCount: Int = 0,
    onClearMemory: () -> Unit = {},
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configurações do Henry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Conecte o Henry ao OpenRouter para usar os modelos gratuitos.")
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("Chave da API") },
                    placeholder = { Text("sk-or-v1-...") },
                    singleLine = true
                )
                if (currentKey.isNotBlank()) {
                    Text("Uma chave já está salva neste aparelho.", color = Color.Gray, fontSize = 12.sp)
                }
                Text("A chave fica salva apenas no armazenamento local do aplicativo.", color = Color.Gray, fontSize = 12.sp)
                HorizontalDivider()
                Text("Memória do Henry: $memoryCount item(ns)", fontWeight = FontWeight.Bold)
                Text("O Henry só grava uma memória quando você pedir com “lembre que...”.", color = Color.Gray, fontSize = 12.sp)
                TextButton(onClick = onClearMemory, enabled = memoryCount > 0) {
                    Text("Apagar memórias")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { if (key.isNotBlank()) onSave(key) }, enabled = key.isNotBlank()) {
                Text("Salvar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}


private fun prepareAttachment(context: android.content.Context, uri: Uri): Attachment? {
    val resolver = context.contentResolver
    val mime = resolver.getType(uri) ?: "application/octet-stream"
    val name = runCatching {
        resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull() ?: "arquivo"

    return runCatching {
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            input.copyTo(output)
            output.toByteArray()
        } ?: return null

        if (mime.startsWith("image/")) {
            Attachment(name, mime, "base64," + Base64.encodeToString(bytes, Base64.NO_WRAP))
        } else if (mime.startsWith("text/") || mime.contains("json") || name.endsWith(".kt") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".md")) {
            Attachment(name, mime, "textbase64:" + Base64.encodeToString(bytes, Base64.NO_WRAP))
        } else {
            Attachment(name, mime, "file")
        }
    }.getOrNull()
}
