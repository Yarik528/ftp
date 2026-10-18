package com.example.stealthfile

import okhttp3.OkHttpClient
import okhttp3.Request
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class NetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
        
    private val gson = Gson()

    suspend fun getFiles(serverIp: String, port: Int): List<Map<String, String>> {
        val url = "http://$serverIp:$port/files"
        val request = Request.Builder().url(url).build()
        
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Ошибка сервера: ${response.code}")
            val json = response.body?.string() ?: "[]"
            val type = object : TypeToken<List<Map<String, String>>>() {}.type
            gson.fromJson(json, type)
        }
    }

    suspend fun downloadFile(
        serverIp: String, 
        port: Int, 
        filePath: String, 
        destinationFile: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ) {
        val url = "http://$serverIp:$port/download/$filePath"
        val request = Request.Builder().url(url).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Ошибка скачивания: ${response.code}")
            
            val body = response.body ?: throw Exception("Пустой ответ")
            val contentLength = body.contentLength()
            
            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    var totalBytesRead: Long = 0
                    
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        onProgress(totalBytesRead, contentLength)
                    }
                    output.flush()
                }
            }
        }
    }
}
