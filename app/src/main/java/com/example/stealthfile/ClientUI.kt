package com.example.stealthfile

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ClientTab(viewModel: ClientViewModel = viewModel()) {
    val ip by viewModel.ip
    val port by viewModel.port
    val files by viewModel.files
    val isLoading by viewModel.isLoading
    val status by viewModel.status
    val downloadProgress by viewModel.downloadProgress
    val uploadProgress by viewModel.uploadProgress
    val isDownloading by viewModel.isDownloading
    val isUploading by viewModel.isUploading
    
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.uploadFile(it, context)
        }
    }

    Column(Modifier.padding(16.dp).fillMaxSize()) {
        Text("Подключение к серверу", style = MaterialTheme.typography.h5, color = Color.White)
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = ip, 
            onValueChange = { viewModel.updateIp(it) }, 
            label = { Text("IP Адрес", color = Color.Gray) }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDownloading && !isUploading,
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
        )
        OutlinedTextField(
            value = port, 
            onValueChange = { viewModel.updatePort(it) }, 
            label = { Text("Порт", color = Color.Gray) }, 
            modifier = Modifier.fillMaxWidth(),
            enabled = !isDownloading && !isUploading,
            colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White)
        )
        
        Spacer(Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { viewModel.connect() },
                enabled = !isLoading && !isDownloading && !isUploading,
                modifier = Modifier.weight(1f)
            ) {
                Text(if (isLoading) "Загрузка..." else "Подключиться", color = Color.Black)
            }
            
            Button(
                onClick = { 
                    filePickerLauncher.launch(arrayOf("*/*"))
                },
                enabled = !isUploading && !isDownloading && ip.isNotEmpty(),
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2196F3))
            ) {
                Text(if (isUploading) "Загрузка..." else "📤 Загрузить", color = Color.White)
            }
        }

        if (isUploading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = uploadProgress,
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2196F3)
            )
            Text("${(uploadProgress * 100).toInt()}%", color = Color.White, fontSize = 12.sp)
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
                                
                                if (isDownloading && file["name"] == files.find { it == file }?.get("name")) {
                                    LinearProgressIndicator(
                                        progress = downloadProgress,
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        color = Color.Cyan
                                    )
                                    Text("${(downloadProgress * 100).toInt()}%", color = Color.Cyan, fontSize = 10.sp)
                                }
                            }
                            Button(
                                onClick = {
                                    viewModel.downloadFile(
                                        file["name"] ?: "",
                                        file["path"] ?: "",
                                        context
                                    )
                                },
                                enabled = !isDownloading && !isUploading
                            ) {
                                Text("⬇️", color = Color.Black)
                            }
                        }
                    }
                }
            }
        }
    }
}
