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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ChatMessage(val text: String, val fromUser: Boolean)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FreedomGPTApp() }
    }
}

@Composable
fun FreedomGPTApp() {
    var draft by remember { mutableStateOf("") }
    var messages by remember {
        mutableStateOf(listOf(ChatMessage("Привет! Это FreedomGPT. Пока работает локальный интерфейс; подключение настоящей AI-модели будет следующим этапом.", false)))
    }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF9B8CFF),
            background = Color(0xFF101116),
            surface = Color(0xFF191B23)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF101116)).padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("FreedomGPT", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("Твоё AI-пространство", color = Color(0xFFAAAAB8), fontSize = 13.sp)
                }
                IconButton(onClick = { messages = emptyList() }) {
                    Icon(Icons.Default.Add, contentDescription = "Новый чат", tint = Color.White)
                }
            }
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
                    ) {
                        Text(
                            message.text,
                            color = Color.White,
                            modifier = Modifier.widthIn(max = 310.dp)
                                .background(
                                    if (message.fromUser) Color(0xFF5145A8) else Color(0xFF20232D),
                                    RoundedCornerShape(18.dp)
                                ).padding(14.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFF20232D), RoundedCornerShape(24.dp)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 16.sp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text("Напиши сообщение…", color = Color(0xFF9294A2))
                        inner()
                    }
                )
                IconButton(onClick = {
                    val text = draft.trim()
                    if (text.isNotEmpty()) {
                        messages = messages + ChatMessage(text, true) +
                            ChatMessage("Интерфейс уже работает. Для ответов настоящей модели нужно подключить сервер AI — это следующий этап разработки.", false)
                        draft = ""
                    }
                }) {
                    Icon(Icons.Default.Send, contentDescription = "Отправить", tint = Color(0xFFB8AEFF))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Версия 0.1.0 • тестовая сборка", color = Color(0xFF777987), fontSize = 11.sp)
        }
    }
}
