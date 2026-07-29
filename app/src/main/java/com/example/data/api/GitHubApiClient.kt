package com.example.data.api

import com.example.data.local.ReleaseAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class GitHubReleaseData(
    val tagName: String,
    val releaseName: String,
    val htmlUrl: String,
    val assets: List<ReleaseAsset>
)

object GitHubApiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    fun extractOwnerAndRepo(inputUrl: String): Pair<String, String>? {
        val clean = inputUrl.trim().removePrefix("https://").removePrefix("http://").removePrefix("github.com/")
        val parts = clean.split("/").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            val owner = parts[0]
            val repo = parts[1].removeSuffix(".git")
            return Pair(owner, repo)
        }
        return null
    }

    suspend fun fetchLatestRelease(owner: String, repo: String): Result<GitHubReleaseData> = withContext(Dispatchers.IO) {
        try {
            val url = "https://release-hub-backend.vercel.app/api/release?owner=$owner&repo=$repo"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ReleaseHub-AndroidApp")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    val errorMsg = try {
                        JSONObject(errorBody).optString("error", "Server error (${response.code})")
                    } catch (_: Exception) {
                        "Server error (${response.code})"
                    }
                    val webRes = fetchLatestReleaseFromWeb(owner, repo)
                    if (webRes.isSuccess) return@withContext webRes

                    return@withContext Result.failure(Exception(errorMsg))
                }

                val bodyStr = response.body?.string() ?: ""
                val jsonObj = JSONObject(bodyStr)
                return@withContext Result.success(parseReleaseJson(jsonObj))
            }
        } catch (e: Exception) {
            val webRes = fetchLatestReleaseFromWeb(owner, repo)
            if (webRes.isSuccess) {
                return@withContext webRes
            }
            Result.failure(e)
        }
    }

    private fun fetchLatestReleaseFromWeb(owner: String, repo: String): Result<GitHubReleaseData> {
        return try {
            val webUrl = "https://github.com/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(webUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.failure(Exception("GitHub API rate limit (403) reached and project web page returned ${response.code}"))
                }
                val finalUrl = response.request.url.toString()
                val html = response.body?.string() ?: ""

                var tagName = finalUrl.substringAfter("/releases/tag/", "").substringBefore("?").trim()
                if (tagName.isBlank() || tagName == finalUrl) {
                    val tagRegex = Regex("""/releases/tag/([^"'\s?]+)""")
                    val match = tagRegex.find(html)
                    tagName = match?.groupValues?.get(1) ?: "latest"
                }

                val assetsList = mutableListOf<ReleaseAsset>()
                val assetRegex = Regex("""href="(/$owner/$repo/releases/download/[^"]+)"""", RegexOption.IGNORE_CASE)
                val matches = assetRegex.findAll(html)
                val addedUrls = mutableSetOf<String>()

                for (m in matches) {
                    val relativePath = m.groupValues[1]
                    val fullUrl = "https://github.com$relativePath"
                    if (addedUrls.add(fullUrl)) {
                        val fileName = relativePath.substringAfterLast("/")
                        if (fileName.isNotBlank()) {
                            assetsList.add(ReleaseAsset(name = fileName, downloadUrl = fullUrl, size = 0L, downloadCount = 0))
                        }
                    }
                }

                if (assetsList.isEmpty()) {
                    val sourceZip = "https://github.com/$owner/$repo/archive/refs/tags/$tagName.zip"
                    assetsList.add(ReleaseAsset(name = "Source code (zip)", downloadUrl = sourceZip, size = 0L, downloadCount = 0))
                }

                Result.success(
                    GitHubReleaseData(
                        tagName = tagName,
                        releaseName = "$repo $tagName",
                        htmlUrl = finalUrl,
                        assets = assetsList
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception("GitHub Rate Limit (403): ${e.localizedMessage}"))
        }
    }

    private fun parseReleaseJson(jsonObj: JSONObject): GitHubReleaseData {
        val tagName = jsonObj.optString("tag_name", jsonObj.optString("tagName", "latest"))
        val releaseName = jsonObj.optString("name", jsonObj.optString("releaseName", tagName))
        val htmlUrl = jsonObj.optString("html_url", jsonObj.optString("htmlUrl", ""))

        val assetsList = mutableListOf<ReleaseAsset>()
        val assetsArr = jsonObj.optJSONArray("assets")
        if (assetsArr != null) {
            for (i in 0 until assetsArr.length()) {
                val assetObj = assetsArr.getJSONObject(i)
                val name = assetObj.optString("name", "asset_$i")
                val downloadUrl = when {
                    assetObj.has("download_url") -> assetObj.optString("download_url", "")
                    assetObj.has("browser_download_url") -> assetObj.optString("browser_download_url", "")
                    assetObj.has("downloadUrl") -> assetObj.optString("downloadUrl", "")
                    else -> ""
                }
                val size = assetObj.optLong("size", 0L)
                val count = when {
                    assetObj.has("download_count") -> assetObj.optInt("download_count", 0)
                    assetObj.has("downloadCount") -> assetObj.optInt("downloadCount", 0)
                    else -> 0
                }
                if (downloadUrl.isNotBlank()) {
                    assetsList.add(ReleaseAsset(name, downloadUrl, size, count))
                }
            }
        }

        return GitHubReleaseData(
            tagName = tagName,
            releaseName = if (releaseName.isBlank()) tagName else releaseName,
            htmlUrl = htmlUrl,
            assets = assetsList
        )
    }

    fun serializeAssets(assets: List<ReleaseAsset>): String {
        val arr = JSONArray()
        for (a in assets) {
            val obj = JSONObject()
            obj.put("name", a.name)
            obj.put("downloadUrl", a.downloadUrl)
            obj.put("size", a.size)
            obj.put("downloadCount", a.downloadCount)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAssets(jsonStr: String?): List<ReleaseAsset> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        val list = mutableListOf<ReleaseAsset>()
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ReleaseAsset(
                        name = obj.optString("name", ""),
                        downloadUrl = obj.optString("downloadUrl", ""),
                        size = obj.optLong("size", 0L),
                        downloadCount = obj.optInt("downloadCount", 0)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }
}
