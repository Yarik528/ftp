package com.example.stealthfile

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MultipartBody
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okio.buffer
import okio.sink
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class NetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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
                    val buffer = ByteArray(8192)
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

    suspend fun uploadFile(
        serverIp: String,
        port: Int,
        file: File,
        onProgress: (Long, Long) -> Unit = { _, _ -> }
    ): Boolean {
        val url = "http://$serverIp:$port/upload"
        
        val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
        
        val progressRequestBody = ProgressRequestBody(requestBody) { bytesWritten, contentLength ->
            onProgress(bytesWritten, contentLength)
        }
        
        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, progressRequestBody)
            .build()
        
        val request = Request.Builder()
            .url(url)
            .post(multipartBody)
            .build()

        return client.newCall(request).execute().use { response ->
            response.isSuccessful
        }
    }
}

class ProgressRequestBody(
    private val delegate: okhttp3.RequestBody,
    private val onProgress: (Long, Long) -> Unit
) : okhttp3.RequestBody() {
    
    override fun contentType() = delegate.contentType()
    
    override fun contentLength() = delegate.contentLength()
    
    override fun writeTo(sink: okio.BufferedSink) {
        val bufferedSink = sink.buffer()
        val countingSink = object : okio.ForwardingSink(bufferedSink) {
            private var bytesWritten = 0L
            private val contentLength = contentLength()
            
            override fun write(source: okio.Buffer, byteCount: Long) {
                super.write(source, byteCount)
                bytesWritten += byteCount
                onProgress(bytesWritten, contentLength)
            }
        }
        
        val bufferedCountingSink = countingSink.buffer()
        delegate.writeTo(bufferedCountingSink)
        bufferedCountingSink.flush()
    }
}
