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
            val url = "https://api.github.com/repos/$owner/$repo/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GitHubDownloader-AndroidApp")
                .header("Accept", "application/vnd.github.v3+json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Fallback to releases array
                    val listUrl = "https://api.github.com/repos/$owner/$repo/releases"
                    val listRequest = Request.Builder()
                        .url(listUrl)
                        .header("User-Agent", "GitHubDownloader-AndroidApp")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                    client.newCall(listRequest).execute().use { listResponse ->
                        if (!listResponse.isSuccessful) {
                            return@withContext Result.failure(Exception("No releases found for $owner/$repo (${response.code})"))
                        }
                        val bodyStr = listResponse.body?.string() ?: ""
                        val jsonArr = JSONArray(bodyStr)
                        if (jsonArr.length() == 0) {
                            return@withContext Result.failure(Exception("No releases available for project $owner/$repo"))
                        }
                        val latestObj = jsonArr.getJSONObject(0)
                        return@withContext Result.success(parseReleaseJson(latestObj))
                    }
                } else {
                    val bodyStr = response.body?.string() ?: ""
                    val jsonObj = JSONObject(bodyStr)
                    return@withContext Result.success(parseReleaseJson(jsonObj))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseReleaseJson(jsonObj: JSONObject): GitHubReleaseData {
        val tagName = jsonObj.optString("tag_name", "latest")
        val releaseName = jsonObj.optString("name", tagName)
        val htmlUrl = jsonObj.optString("html_url", "")

        val assetsList = mutableListOf<ReleaseAsset>()
        val assetsArr = jsonObj.optJSONArray("assets")
        if (assetsArr != null) {
            for (i in 0 until assetsArr.length()) {
                val assetObj = assetsArr.getJSONObject(i)
                val name = assetObj.optString("name", "asset_$i")
                val downloadUrl = assetObj.optString("browser_download_url", "")
                val size = assetObj.optLong("size", 0L)
                val count = assetObj.optInt("download_count", 0)
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
