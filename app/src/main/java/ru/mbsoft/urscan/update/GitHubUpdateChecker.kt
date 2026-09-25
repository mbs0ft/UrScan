package ru.mbsoft.urscan.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    object UpToDate : UpdateCheckResult()
    data class Available(val newVersion: String, val downloadUrl: String, val releaseNotes: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

object GitHubUpdateChecker {

    const val GITHUB_REPO_URL = "https://github.com/mbs0ft/UrScan"
    private const val API_URL = "https://api.github.com/repos/mbs0ft/UrScan/releases/latest"

    suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(API_URL)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "UrScan-Android-App")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("HTTP ${connection.responseCode}")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(response)
            val tagName = json.optString("tag_name", "").trim().removePrefix("v")
            val body = json.optString("body", "")

            var apkUrl: String? = null
            val assets = json.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name", "")
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkUrl = asset.optString("browser_download_url")
                        break
                    }
                }
            }

            if (tagName.isNotEmpty() && isNewerVersion(tagName, currentVersion) && !apkUrl.isNullOrEmpty()) {
                UpdateCheckResult.Available(newVersion = tagName, downloadUrl = apkUrl, releaseNotes = body)
            } else {
                UpdateCheckResult.UpToDate
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Network error")
        } finally {
            connection?.disconnect()
        }
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val remoteClean = remote.removePrefix("v").trim()
        val currentClean = current.removePrefix("v").trim()
        val remoteParts = remoteClean.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = currentClean.split(".").mapNotNull { it.toIntOrNull() }

        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }

    suspend fun downloadApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Int) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            var targetUrl = downloadUrl
            var redirectCount = 0
            var finalConnection: HttpURLConnection? = null

            while (redirectCount < 5) {
                val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                    setRequestProperty("User-Agent", "UrScan-Android-App")
                    connectTimeout = 15000
                    readTimeout = 30000
                    instanceFollowRedirects = false
                }
                val code = conn.responseCode
                if (code in listOf(301, 302, 303, 307, 308)) {
                    val location = conn.getHeaderField("Location")
                    conn.disconnect()
                    if (location != null) {
                        targetUrl = location
                        redirectCount++
                        continue
                    }
                }
                finalConnection = conn
                break
            }

            if (finalConnection == null) return@withContext null
            connection = finalConnection

            val totalBytes = connection.contentLength
            val cacheDir = File(context.cacheDir, "updates").apply { if (!exists()) mkdirs() }
            val apkFile = File(cacheDir, "UrScan-update.apk")
            if (apkFile.exists()) apkFile.delete()

            connection.inputStream.use { input ->
                FileOutputStream(apkFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    var totalRead = 0L
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        totalRead += read
                        if (totalBytes > 0) {
                            val progress = ((totalRead * 100) / totalBytes).toInt()
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }
            apkFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    fun installApk(context: Context, apkFile: File) {
        try {
            val apkUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
