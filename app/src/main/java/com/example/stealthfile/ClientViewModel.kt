package com.example.stealthfile

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.State
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ClientViewModel : ViewModel() {
    
    private val client = NetworkClient()
    
    private val _ip = mutableStateOf("")
    val ip: State<String> = _ip
    
    private val _port = mutableStateOf("8080")
    val port: State<String> = _port
    
    private val _files = mutableStateOf<List<Map<String, String>>>(emptyList())
    val files: State<List<Map<String, String>>> = _files
    
    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading
    
    private val _status = mutableStateOf("")
    val status: State<String> = _status
    
    private val _downloadProgress = mutableStateOf(0f)
    val downloadProgress: State<Float> = _downloadProgress
    
    private val _uploadProgress = mutableStateOf(0f)
    val uploadProgress: State<Float> = _uploadProgress
    
    private val _isDownloading = mutableStateOf(false)
    val isDownloading: State<Boolean> = _isDownloading
    
    private val _isUploading = mutableStateOf(false)
    val isUploading: State<Boolean> = _isUploading

    fun updateIp(value: String) {
        _ip.value = value
    }

    fun updatePort(value: String) {
        _port.value = value
    }

    fun connect() {
        viewModelScope.launch {
            _isLoading.value = true
            _status.value = "Подключение..."
            try {
                val result = withContext(Dispatchers.IO) {
                    client.getFiles(_ip.value, _port.value.toIntOrNull() ?: 8080)
                }
                _files.value = result
                _status.value = "Найдено файлов: ${result.size}"
            } catch (e: Exception) {
                _status.value = "Ошибка: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun downloadFile(fileName: String, filePath: String, context: Context) {
        viewModelScope.launch {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            _status.value = "Скачивание $fileName..."
            
            try {
                val dest = File(context.getExternalFilesDir(null), fileName)
                
                withContext(Dispatchers.IO) {
                    client.downloadFile(
                        serverIp = _ip.value,
                        port = _port.value.toIntOrNull() ?: 8080,
                        filePath = filePath,
                        destinationFile = dest
                    ) { bytesDownloaded, totalBytes ->
                        val progress = if (totalBytes > 0) {
                            bytesDownloaded.toFloat() / totalBytes.toFloat()
                        } else 0f
                        _downloadProgress.value = progress
                    }
                }
                
                _status.value = "✅ Сохранено: ${dest.absolutePath}"
            } catch (e: Exception) {
                _status.value = "❌ Ошибка: ${e.message}"
            } finally {
                _isDownloading.value = false
                _downloadProgress.value = 0f
            }
        }
    }

    fun uploadFile(uri: Uri, context: Context) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            _status.value = "Загрузка файла..."
            
            try {
                // Копируем файл из URI во временную папку
                val tempFile = File(context.cacheDir, getFileNameFromUri(uri, context))
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                
                val success = withContext(Dispatchers.IO) {
                    client.uploadFile(
                        serverIp = _ip.value,
                        port = _port.value.toIntOrNull() ?: 8080,
                        file = tempFile
                    ) { bytesUploaded, totalBytes ->
                        val progress = if (totalBytes > 0) {
                            bytesUploaded.toFloat() / totalBytes.toFloat()
                        } else 0f
                        _uploadProgress.value = progress
                    }
                }
                
                tempFile.delete()
                
                if (success) {
                    _status.value = "✅ Файл загружен на сервер"
                    // Обновляем список файлов
                    connect()
                } else {
                    _status.value = "❌ Ошибка загрузки"
                }
            } catch (e: Exception) {
                _status.value = "❌ Ошибка: ${e.message}"
            } finally {
                _isUploading.value = false
                _uploadProgress.value = 0f
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri, context: Context): String {
        var fileName = "uploaded_file_${System.currentTimeMillis()}"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName
    }
}
