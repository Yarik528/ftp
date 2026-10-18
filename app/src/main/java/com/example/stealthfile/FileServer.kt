package com.example.stealthfile

import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoHTTPD.IHTTPSession
import fi.iki.elonen.NanoHTTPD.Response
import fi.iki.elonen.NanoHTTPD.Response.Status
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

class FileServer(private val port: Int) : NanoHTTPD(port) {

    private val gson = Gson()
    private val rootDir = File("/storage/emulated/0/Download")
    
    val logs = mutableListOf<String>()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val ip = session.remoteIpAddress
        
        addLog("[$ip] Запрос: $uri")

        return try {
            when {
                uri == "/" || uri == "/files" -> {
                    val files = listFiles(rootDir)
                    val json = gson.toJson(files)
                    newFixedLengthResponse(Status.OK, "application/json", json)
                }
                uri.startsWith("/download/") -> {
                    val relativePath = uri.removePrefix("/download/")
                    val file = File(rootDir, relativePath).canonicalFile
                    
                    if (file.exists() && file.isFile && file.path.startsWith(rootDir.canonicalPath)) {
                        val mimeType = getMimeType(file.name)
                        newChunkedResponse(Status.OK, mimeType, FileInputStream(file))
                    } else {
                        newFixedLengthResponse(Status.FORBIDDEN, "text/plain", "Access Denied or File Not Found")
                    }
                }
                else -> {
                    newFixedLengthResponse(Status.NOT_FOUND, "text/plain", "404 Not Found")
                }
            }
        } catch (e: Exception) {
            addLog("Ошибка: ${e.message}")
            newFixedLengthResponse(Status.INTERNAL_ERROR, "text/plain", "Server Error: ${e.message}")
        }
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        logs.add("$time $message")
        if (logs.size > 100) logs.removeAt(0)
    }

    private fun listFiles(dir: File): List<Map<String, String>> {
        val result = mutableListOf<Map<String, String>>()
        dir.listFiles()?.forEach { file ->
            if (file.isFile && !file.name.startsWith(".")) {
                result.add(mapOf(
                    "name" to file.name,
                    "path" to file.name,
                    "size" to formatSize(file.length()),
                    "date" to SimpleDateFormat("dd.MM HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                ))
            }
        }
        return result.sortedByDescending { it["date"] }
    }

    private fun formatSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            else -> "${size / (1024 * 1024)} MB"
        }
    }

    private fun getMimeType(fileName: String): String {
        return when {
            fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") -> "image/jpeg"
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".mp4") -> "video/mp4"
            fileName.endsWith(".pdf") -> "application/pdf"
            fileName.endsWith(".txt") -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
