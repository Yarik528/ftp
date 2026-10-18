package com.example.stealthfile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface

class MainActivity : ComponentActivity() {
    
    private var server: FileServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colors = darkColors()) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF121212)) {
                    AppScreen(
                        onStartServer = { port -> startServer(port) },
                        onStopServer = { stopServer() },
                        serverLogs = server?.logs ?: emptyList()
                    )
                }
            }
        }
    }

    private fun startServer(port: Int) {
        try {
            server = FileServer(port)
            server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Toast.makeText(this, "Сервер запущен на порту $port", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
        Toast.makeText(this, "Сервер остановлен", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun AppScreen(onStartServer: (Int) -> Unit, onStopServer: () -> Unit, serverLogs: List<String>) {
    var selectedTab by remember { mutableStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab, backgroundColor = Color(0xFF1E1E1E), contentColor = Color.White) {
            Tab(text = { Text("🖥️ Сервер") }, selected = selectedTab == 0, onClick = { selectedTab = 0 })
            Tab(text = { Text("📱 Клиент") }, selected = selectedTab == 1, onClick = { selectedTab = 1 })
        }

        if (selectedTab == 0) {
            ServerTab(onStart = onStartServer, onStop = onStopServer, logs = serverLogs)
        } else {
            ClientTab()
        }
    }
}

@Composable
fun ServerTab(onStart: (Int) -> Unit, onStop: () -> Unit, logs: List<String>) {
    var port by remember { mutableStateOf("8080") }
    var isRunning by remember { mutableStateOf(false) }
    val ip = getLocalIpAddress()

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Управление сервером", style = MaterialTheme.typography.h5, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = port, 
            onValueChange = { port = it }, 
            label = { Text("Порт", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (!isRunning) {
                    onStart(port.toIntOrNull() ?: 8080)
                    isRunning = true
                } else {
                    onStop()
                    isRunning = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(backgroundColor = if(isRunning) Color.Red else Color.Green)
        ) {
            Text(if (isRunning) "Остановить сервер" else "Запустить сервер", color = Color.Black)
        }

        if (isRunning) {
            Spacer(Modifier.height(16.dp))
            Card(backgroundColor = Color(0xFF2C2C2C), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Text("Ваш IP адрес:", color = Color.Gray, fontSize = 12.sp)
                    Text(ip, color = Color.Cyan, fontFamily = FontFamily.Monospace, fontSize = 18.sp)
                    Text("Откройте клиент на другом устройстве и введите этот IP", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Логи подключений:", color = Color.White, style = MaterialTheme.typography.subtitle1)
        Spacer(Modifier.height(8.dp))
        
        Box(modifier = Modifier.weight(1f).background(Color.Black).padding(8.dp)) {
            LazyColumn {
                items(logs) { log ->
                    Text(log, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                }
            }
        }
    }
}

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

fun getLocalIpAddress(): String {
    try {
        for (en in NetworkInterface.getNetworkInterfaces()) {
            for (addr in en.inetAddresses) {
                if (!addr.isLoopbackAddress && addr is InetAddress && addr.hostAddress.indexOf(':') == -1) {
                    return addr.hostAddress
                }
            }
        }
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
    return "Не удалось определить"
}
