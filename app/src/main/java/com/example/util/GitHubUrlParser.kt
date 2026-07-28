package com.example.util

import android.webkit.MimeTypeMap
import java.net.URI

object GitHubUrlParser {

    fun parseAndNormalizeUrl(rawUrl: String): NormalizedUrlInfo {
        val trimmed = rawUrl.trim()
        if (trimmed.isEmpty()) {
            return NormalizedUrlInfo(
                originalUrl = rawUrl,
                downloadUrl = "",
                fileName = "downloaded_file",
                isGitHubUrl = false,
                mimeType = "*/*"
            )
        }

        var normalizedUrl = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else {
            trimmed
        }

        // Handle GitHub blob URL to raw URL transformation
        // Example: https://github.com/owner/repo/blob/main/path/file.apk -> https://raw.githubusercontent.com/owner/repo/main/path/file.apk
        if (normalizedUrl.contains("github.com") && normalizedUrl.contains("/blob/")) {
            normalizedUrl = normalizedUrl.replace("github.com", "raw.githubusercontent.com")
                .replace("/blob/", "/")
        }

        val fileName = extractFileName(normalizedUrl)
        val mimeType = getMimeTypeFromFileName(fileName)
        val isGitHub = normalizedUrl.contains("github.com") || normalizedUrl.contains("githubusercontent.com")

        return NormalizedUrlInfo(
            originalUrl = rawUrl,
            downloadUrl = normalizedUrl,
            fileName = fileName,
            isGitHubUrl = isGitHub,
            mimeType = mimeType
        )
    }

    fun extractFileName(url: String): String {
        return try {
            val uri = URI(url)
            val path = uri.path ?: return "downloaded_file"
            var name = path.substringAfterLast('/')
            
            // Strip matrix params or query strings if present
            if (name.contains('?')) {
                name = name.substringBefore('?')
            }
            if (name.isBlank() || name == "/") {
                "downloaded_file"
            } else {
                name
            }
        } catch (e: Exception) {
            val lastPart = url.substringAfterLast('/').substringBefore('?').substringBefore('#')
            if (lastPart.isNotBlank()) lastPart else "downloaded_file"
        }
    }

    fun getMimeTypeFromFileName(fileName: String): String {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        if (extension.isBlank()) return "*/*"

        if (extension == "apk") {
            return "application/vnd.android.package-archive"
        }

        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        return mime ?: when (extension) {
            "zip" -> "application/zip"
            "tar", "gz", "tgz" -> "application/gzip"
            "7z" -> "application/x-7z-compressed"
            "rar" -> "application/x-rar-compressed"
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "mp4" -> "video/mp4"
            "mp3" -> "audio/mpeg"
            "json" -> "application/json"
            "txt" -> "text/plain"
            else -> "*/*"
        }
    }
}

data class NormalizedUrlInfo(
    val originalUrl: String,
    val downloadUrl: String,
    val fileName: String,
    val isGitHubUrl: Boolean,
    val mimeType: String
)
