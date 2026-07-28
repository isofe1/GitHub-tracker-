package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PAUSED
}

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val fileSize: Long = 0L,
    val downloadedBytes: Long = 0L,
    val mimeType: String = "*/*",
    val status: DownloadStatus = DownloadStatus.IDLE,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val speedBps: Long = 0L
) {
    val progressPercentage: Float
        get() = if (fileSize > 0) (downloadedBytes.toFloat() / fileSize.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedSize: String
        get() = formatBytes(fileSize)

    val formattedDownloadedSize: String
        get() = formatBytes(downloadedBytes)

    val formattedSpeed: String
        get() = if (speedBps > 0) "${formatBytes(speedBps)}/s" else ""

    val fileExtension: String
        get() = fileName.substringAfterLast('.', "").uppercase()

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            if (kb < 1024) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            if (mb < 1024) return String.format("%.1f MB", mb)
            val gb = mb / 1024.0
            return String.format("%.2f GB", gb)
        }
    }
}
