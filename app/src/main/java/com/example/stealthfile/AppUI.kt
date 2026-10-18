package com.example.stealthfile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import java.net.InetAddress
import java.net.NetworkInterface

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
