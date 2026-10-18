package com.example.stealthfile

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import fi.iki.elonen.NanoHTTPD

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
