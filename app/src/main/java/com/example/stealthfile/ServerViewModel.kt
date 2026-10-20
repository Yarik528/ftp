package com.example.stealthfile

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import androidx.lifecycle.viewModelScope
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ServerViewModel(application: Application) : AndroidViewModel(application) {
    
    private var server: FileServer? = null
    
    private val _isRunning = mutableStateOf(false)
    val isRunning: State<Boolean> = _isRunning
    
    private val _logs = mutableStateListOf<String>()
    val logs: List<String> get() = _logs
    
    private val _port = mutableStateOf("8080")
    val port: State<String> = _port

    fun updatePort(value: String) {
        _port.value = value
    }

    fun startServer() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val portInt = _port.value.toIntOrNull() ?: 8080
                    server = FileServer(portInt)
                    server?.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
                    _isRunning.value = true
                    addLog("✅ Сервер запущен на порту $portInt")
                    
                    // Запускаем логгер для обновления UI
                    launchLogUpdater()
                    
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Сервер запущен", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    addLog("❌ Ошибка: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Ошибка: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                server?.stop()
                server = null
                _isRunning.value = false
                addLog("⏹️ Сервер остановлен")
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Сервер остановлен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun launchLogUpdater() {
        viewModelScope.launch {
            while (_isRunning.value) {
                server?.logs?.let { serverLogs ->
                    val newLogs = serverLogs.filter { it !in _logs }
                    if (newLogs.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _logs.addAll(newLogs)
                        }
                    }
                }
                kotlinx.coroutines.delay(500)
            }
        }
    }

    private fun addLog(message: String) {
        _logs.add(message)
        if (_logs.size > 100) {
            _logs.removeAt(0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        server?.stop()
    }
}
