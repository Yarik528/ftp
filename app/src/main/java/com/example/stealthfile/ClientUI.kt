package com.example.stealthfile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ClientTab() {
    val scope = rememberCoroutineScope()
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8080") }
    var files by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val context = LocalContext.current
    val client = NetworkClient()

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Подключение к серверу", style = MaterialTheme.typography.h5, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP Адрес", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White))
        OutlinedTextField(value = port, onValueChange = { port = it }, label = { Text("Порт", color = Color.Gray) }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White))
        
        Spacer(Modifier.height(8.dp))
        
        Button(onClick = {
            scope.launch {
                isLoading = true
                status = "Подключение..."
                try {
                    files = client.getFiles(ip, port.toIntOrNull() ?: 8080)
                    status = "Найдено файлов: ${files.size}"
                } catch (e: Exception) {
                    status = "Ошибка: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        }, enabled = !isLoading, modifier = Modifier.fillMaxWidth()) {
            Text(if (isLoading) "Загрузка..." else "Подключиться", color = Color.Black)
        }

        Spacer(Modifier.height(8.dp))
        Text(status, color = if (status.contains("Ошибка")) Color.Red else Color.Green)

        if (files.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(files) { file ->
                    Card(
                        backgroundColor = Color(0xFF2C2C2C), 
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(file["name"] ?: "Unknown", color = Color.White, maxLines = 1)
                                Text("${file["size"]} • ${file["date"]}", color = Color.Gray, fontSize = 12.sp)
                            }
                            Button(onClick = {
                                scope.launch {
                                    try {
                                        val dest = File(context.getExternalFilesDir(null), file["name"])
                                        status = "Скачивание ${file["name"]}..."
                                        client.downloadFile(ip, port.toIntOrNull() ?: 8080, file["path"]!!, dest)
                                        status = "✅ Сохранено: ${dest.absolutePath}"
                                    } catch (e: Exception) {
                                        status = "❌ Ошибка: ${e.message}"
                                    }
                                }
                            }) {
                                Text("⬇️", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
