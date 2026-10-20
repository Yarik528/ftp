package com.example.stealthfile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ServerTab(viewModel: ServerViewModel) {
    val isRunning by viewModel.isRunning
    val port by viewModel.port
    val logs = viewModel.logs
    val ip = getLocalIpAddress()

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Управление сервером", style = MaterialTheme.typography.h5, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = port, 
            onValueChange = { viewModel.updatePort(it) }, 
            label = { Text("Порт", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isRunning,
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.Cyan)
        )
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = { 
                if (!isRunning) {
                    viewModel.startServer()
                } else {
                    viewModel.stopServer()
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
