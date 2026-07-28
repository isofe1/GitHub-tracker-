package com.example.service

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.local.DownloadItem
import com.example.data.local.DownloadRepository
import com.example.data.local.DownloadStatus
import com.example.util.GitHubUrlParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class DownloadEngine(
    private val context: Context,
    private val repository: DownloadRepository
) {
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val downloadJobs = ConcurrentHashMap<Long, Job>()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentActiveDownload = MutableStateFlow<DownloadItem?>(null)
    val currentActiveDownload: StateFlow<DownloadItem?> = _currentActiveDownload.asStateFlow()

    suspend fun downloadFile(
        rawUrl: String,
        customFileName: String? = null,
        useSystemDownloadManager: Boolean = false
    ): Result<DownloadItem> {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedInfo = GitHubUrlParser.parseAndNormalizeUrl(rawUrl)
                val fileName = if (!customFileName.isNullOrBlank()) customFileName else normalizedInfo.fileName
                
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }

                // Ensure unique file name if file already exists
                var targetFile = File(downloadsDir, fileName)
                var fileIndex = 1
                val baseName = fileName.substringBeforeLast('.')
                val extension = if (fileName.contains('.')) ".${fileName.substringAfterLast('.')}" else ""

                while (targetFile.exists()) {
                    targetFile = File(downloadsDir, "${baseName}_$fileIndex$extension")
                    fileIndex++
                }

                val initialItem = DownloadItem(
                    url = normalizedInfo.downloadUrl,
                    fileName = targetFile.name,
                    filePath = targetFile.absolutePath,
                    mimeType = normalizedInfo.mimeType,
                    status = DownloadStatus.DOWNLOADING,
                    timestamp = System.currentTimeMillis()
                )

                val id = repository.insert(initialItem)
                val activeItem = initialItem.copy(id = id)
                _currentActiveDownload.value = activeItem

                if (useSystemDownloadManager) {
                    enqueueSystemDownload(activeItem)
                } else {
                    startInAppDownload(activeItem)
                }

                Result.success(activeItem)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun startInAppDownload(item: DownloadItem) {
        val job = scope.launch {
            var outputStream: FileOutputStream? = null
            var inputStream: InputStream? = null
            
            try {
                val request = Request.Builder()
                    .url(item.url)
                    .header("User-Agent", "Mozilla/5.0 (Android; GitHubDownloader)")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorMsg = "HTTP error code: ${response.code}"
                    val failedItem = item.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = errorMsg
                    )
                    repository.update(failedItem)
                    _currentActiveDownload.value = failedItem
                    return@launch
                }

                val body = response.body
                if (body == null) {
                    val failedItem = item.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "Empty response body"
                    )
                    repository.update(failedItem)
                    _currentActiveDownload.value = failedItem
                    return@launch
                }

                val totalLength = body.contentLength()
                val mimeType = response.header("Content-Type") ?: item.mimeType

                val updatedHeaderItem = item.copy(
                    fileSize = if (totalLength > 0) totalLength else item.fileSize,
                    mimeType = mimeType
                )
                repository.update(updatedHeaderItem)

                val file = File(item.filePath)
                outputStream = FileOutputStream(file)
                inputStream = body.byteStream()

                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesDownloaded = 0L
                var lastTime = System.currentTimeMillis()
                var lastBytes = 0L
                var currentSpeed = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesDownloaded += bytesRead

                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastTime

                    if (timeDiff >= 500) { // Update status every 500ms
                        val bytesDiff = totalBytesDownloaded - lastBytes
                        currentSpeed = (bytesDiff * 1000) / timeDiff

                        lastTime = currentTime
                        lastBytes = totalBytesDownloaded

                        val progressItem = updatedHeaderItem.copy(
                            downloadedBytes = totalBytesDownloaded,
                            speedBps = currentSpeed,
                            status = DownloadStatus.DOWNLOADING
                        )
                        repository.update(progressItem)
                        _currentActiveDownload.value = progressItem
                    }
                }

                outputStream.flush()

                val completedItem = updatedHeaderItem.copy(
                    downloadedBytes = totalBytesDownloaded,
                    fileSize = if (totalLength > 0) totalLength else totalBytesDownloaded,
                    status = DownloadStatus.COMPLETED,
                    speedBps = 0L
                )
                repository.update(completedItem)
                _currentActiveDownload.value = completedItem

            } catch (e: Exception) {
                val failedItem = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed",
                    speedBps = 0L
                )
                repository.update(failedItem)
                _currentActiveDownload.value = failedItem
            } finally {
                try {
                    inputStream?.close()
                    outputStream?.close()
                } catch (_: Exception) {}
                downloadJobs.remove(item.id)
            }
        }

        downloadJobs[item.id] = job
    }

    private fun enqueueSystemDownload(item: DownloadItem) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(item.url))
                .setTitle(item.fileName)
                .setDescription("Downloading file from GitHub")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, item.fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            scope.launch {
                var downloading = true
                while (downloading) {
                    delay(1000)
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)
                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val totalSizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1
                        val downloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                        val total = if (totalSizeIndex >= 0) cursor.getLong(totalSizeIndex) else 0L

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                downloading = false
                                val updated = item.copy(
                                    downloadedBytes = total,
                                    fileSize = total,
                                    status = DownloadStatus.COMPLETED
                                )
                                repository.update(updated)
                                _currentActiveDownload.value = updated
                            }
                            DownloadManager.STATUS_FAILED -> {
                                downloading = false
                                val updated = item.copy(
                                    status = DownloadStatus.FAILED,
                                    errorMessage = "System DownloadManager failed"
                                )
                                repository.update(updated)
                                _currentActiveDownload.value = updated
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val updated = item.copy(
                                    downloadedBytes = downloaded,
                                    fileSize = total,
                                    status = DownloadStatus.DOWNLOADING
                                )
                                repository.update(updated)
                                _currentActiveDownload.value = updated
                            }
                        }
                    } else {
                        downloading = false
                    }
                    cursor?.close()
                }
            }
        } catch (e: Exception) {
            scope.launch {
                val failed = item.copy(
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "System download enqueue error"
                )
                repository.update(failed)
                _currentActiveDownload.value = failed
            }
        }
    }

    fun cancelDownload(id: Long) {
        downloadJobs[id]?.cancel()
        downloadJobs.remove(id)
        scope.launch {
            val item = repository.getDownloadById(id)
            if (item != null) {
                val cancelled = item.copy(
                    status = DownloadStatus.PAUSED,
                    speedBps = 0L
                )
                repository.update(cancelled)
                if (_currentActiveDownload.value?.id == id) {
                    _currentActiveDownload.value = cancelled
                }
            }
        }
    }

    fun openDownloadedFile(item: DownloadItem): String? {
        val file = File(item.filePath)
        if (!file.exists()) {
            return "File not found at path: ${item.filePath}"
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Cannot open file"
        }
    }

    fun shareDownloadedFile(item: DownloadItem): String? {
        val file = File(item.filePath)
        if (!file.exists()) {
            return "File not found"
        }

        return try {
            val authority = "${context.packageName}.fileprovider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${item.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            null
        } catch (e: Exception) {
            e.localizedMessage ?: "Cannot share file"
        }
    }
}
