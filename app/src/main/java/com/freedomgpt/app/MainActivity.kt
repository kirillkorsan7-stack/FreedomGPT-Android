package com.freedomgpt.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FreedomGPTApp(
                loadHistory = {
                    val raw = getSharedPreferences("freedomgpt", MODE_PRIVATE).getString("history", null)
                    if (raw.isNullOrBlank()) emptyList() else runCatching {
                        val array = JSONArray(raw)
                        (0 until array.length()).map { i ->
                            val item = array.getJSONObject(i)
                            ChatMessage(item.getString("text"), item.getBoolean("fromUser"))
                        }
                    }.getOrDefault(emptyList())
                },
                saveHistory = { history ->
                    val array = JSONArray()
                    history.forEach { item ->
                        array.put(JSONObject().put("text", item.text).put("fromUser", item.fromUser))
                    }
                    getSharedPreferences("freedomgpt", MODE_PRIVATE).edit().putString("history", array.toString()).apply()
                },
                loadEndpoint = {
                    getSharedPreferences("freedomgpt", MODE_PRIVATE).getString("endpoint", "") ?: ""
                },
                saveEndpoint = { endpoint ->
                    getSharedPreferences("freedomgpt", MODE_PRIVATE).edit().putString("endpoint", endpoint).apply()
                }
            )
        }
    }
}

@Composable
fun FreedomGPTApp(
    loadHistory: () -> List<ChatMessage>,
    saveHistory: (List<ChatMessage>) -> Unit,
    loadEndpoint: () -> String,
    saveEndpoint: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(loadHistory()) }
    var endpoint by remember { mutableStateOf(loadEndpoint()) }
    var endpointDraft by remember { mutableStateOf(endpoint) }
    var showSettings by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages) { saveHistory(messages) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF9B8CFF), background = Color(0xFF101116), surface = Color(0xFF191B23))) {
        Column(Modifier.fillMaxSize().background(Color(0xFF101116)).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("FreedomGPT", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(if (endpoint.isBlank()) "Локальная история • AI не подключён" else "AI-сервер настроен", color = Color(0xFFAAAAB8), fontSize = 12.sp)
                }
                IconButton(onClick = { showSettings = true; endpointDraft = endpoint }) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки", tint = Color.White)
                }
                IconButton(onClick = { messages = emptyList(); error = null }) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат", tint = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(messages) { message ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start) {
                        Text(message.text, color = Color.White, modifier = Modifier.widthIn(max = 310.dp)
                            .background(if (message.fromUser) Color(0xFF5145A8) else Color(0xFF20232D), RoundedCornerShape(18.dp)).padding(14.dp))
                    }
                }
                if (sending) item { Text("FreedomGPT печатает…", color = Color(0xFFAAAAB8), fontSize = 13.sp) }
            }
            error?.let { Text(it, color = Color(0xFFFF9A9A), fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp)) }
            Row(Modifier.fillMaxWidth().background(Color(0xFF20232D), RoundedCornerShape(24.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    decorationBox = { inner -> if (draft.isEmpty()) Text("Напиши сообщение…", color = Color(0xFF9294A2)); inner() })
                IconButton(enabled = !sending, onClick = {
                    val prompt = draft.trim()
                    if (prompt.isNotEmpty()) {
                        draft = ""
                        error = null
                        val updated = messages + ChatMessage(prompt, true)
                        messages = updated
                        if (endpoint.isBlank()) {
                            messages = updated + ChatMessage("Сначала укажи адрес своего AI-сервера в настройках. История переписки сохраняется на устройстве.", false)
                        } else {
                            sending = true
                            scope.launch {
                                try {
                                    val answer = requestFreedomGpt(endpoint, updated)
                                    messages = messages + ChatMessage(answer, false)
                                } catch (e: Exception) {
                                    error = e.message ?: "Не удалось связаться с сервером."
                                } finally { sending = false }
                            }
                        }
                    }
                }) { Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color(0xFFB8AEFF)) }
            }
            Spacer(Modifier.height(8.dp))
            Text("Версия 0.2.0 • история хранится на устройстве", color = Color(0xFF777987), fontSize = 11.sp)
        }

        if (showSettings) {
            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Подключение AI-сервера") },
                text = {
                    Column {
                        Text("Вставь HTTPS-адрес backend-сервера, заканчивающийся на /chat. API-ключ сюда не вводи.", fontSize = 13.sp)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(value = endpointDraft, onValueChange = { endpointDraft = it }, label = { Text("Адрес сервера") }, singleLine = true)
                    }
                },
                confirmButton = { TextButton(onClick = {
                    endpoint = endpointDraft.trim().trimEnd('/')
                    saveEndpoint(endpoint)
                    showSettings = false
                }) { Text("Сохранить") } },
                dismissButton = { TextButton(onClick = { showSettings = false }) { Text("Отмена") } }
            )
        }
    }
}

private suspend fun requestFreedomGpt(endpoint: String, messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
    val url = URL(endpoint)
    require(url.protocol == "https") { "Для безопасности нужен HTTPS-адрес сервера." }
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 15000
        readTimeout = 90000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
    }
    try {
        val items = JSONArray()
        messages.takeLast(30).forEach { msg ->
            items.put(JSONObject().put("role", if (msg.fromUser) "user" else "assistant").put("content", msg.text))
        }
        val body = JSONObject().put("messages", items).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        if (code !in 200..299) throw IllegalStateException("Сервер ответил с ошибкой HTTP $code.")
        JSONObject(response).optString("reply").takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Сервер вернул пустой ответ.")
    } finally { connection.disconnect() }
}
