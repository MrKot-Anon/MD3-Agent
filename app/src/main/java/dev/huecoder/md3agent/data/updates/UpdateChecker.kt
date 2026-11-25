package dev.huecoder.md3agent.data.updates

import android.content.Context
import dev.huecoder.md3agent.net.fetchText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

sealed interface UpdateStatus {
    data class UpToDate(val latest: String) : UpdateStatus
    data class Available(val version: String, val pageUrl: String, val apkUrl: String?, val sha256: String? = null) : UpdateStatus
    data object NotPublished : UpdateStatus
    data object Unreachable : UpdateStatus
}

class UpdateChecker(private val context: Context, private val installedVersion: String) {
    private val client = OkHttpClient.Builder()
        .addNetworkInterceptor { chain ->
            if (!UpdateHostPolicy.permits(chain.request().url.host, chain.request().url.encodedPath)) {
                throw IOException("Update request went outside this fork's GitHub release")
            }
            chain.proceed(chain.request())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun check(): UpdateStatus {
      return try {
        val request = Request.Builder().url(API_URL).header("Accept", "application/vnd.github+json").build()
        val body = client.fetchText(request)
        val release = Json.parseToJsonElement(body).jsonObject
        val tag = release["tag_name"]?.jsonPrimitive?.content.orEmpty()
        val newest = AppVersion.parse(tag) ?: return UpdateStatus.Unreachable
        val current = AppVersion.parse(installedVersion) ?: return UpdateStatus.Unreachable
        if (newest <= current) return UpdateStatus.UpToDate(installedVersion)
        val pageUrl = release["html_url"]?.jsonPrimitive?.content.orEmpty()
        if (!pageUrl.startsWith("$REPOSITORY_URL/releases/tag/")) return UpdateStatus.Unreachable
        val asset = release["assets"]?.jsonArray?.firstOrNull {
            it.jsonObject["name"]?.jsonPrimitive?.content?.endsWith(".apk") == true
        }?.jsonObject ?: return UpdateStatus.NotPublished
        val apkUrl = asset["browser_download_url"]?.jsonPrimitive?.content.orEmpty()
        if (!apkUrl.startsWith("$REPOSITORY_URL/releases/download/$tag/") || !apkUrl.endsWith(".apk")) return UpdateStatus.Unreachable
        val sha = asset["digest"]?.jsonPrimitive?.content?.removePrefix("sha256:")
        if (sha?.matches(Regex("[0-9a-fA-F]{64}")) != true) return UpdateStatus.Unreachable
        UpdateStatus.Available(tag.removePrefix("v"), pageUrl, apkUrl, sha.lowercase())
      } catch (cancelled: CancellationException) {
        throw cancelled
      } catch (_: Exception) {
        UpdateStatus.Unreachable
      }
    }

    suspend fun download(offer: UpdateStatus.Available): File = withContext(Dispatchers.IO) {
        val url = offer.apkUrl ?: throw IOException("Release has no APK")
        if (!url.startsWith("$REPOSITORY_URL/releases/download/") || !url.endsWith(".apk")) throw IOException("Unexpected APK URL")
        val expected = offer.sha256 ?: throw IOException("Release has no APK checksum")
        val destination = File(context.cacheDir, "updates/${offer.version.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.apk")
        destination.parentFile?.mkdirs()
        val partial = File(destination.path + ".part")
        partial.delete()
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("APK download failed: HTTP ${response.code}")
                val digest = MessageDigest.getInstance("SHA-256")
                var total = 0L
                response.body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            if (total > MAX_APK_BYTES) throw IOException("APK exceeds size limit")
                            digest.update(buffer, 0, count)
                            output.write(buffer, 0, count)
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (actual != expected) throw IOException("APK SHA-256 differs from GitHub release")
            }
            destination.delete()
            if (!partial.renameTo(destination)) throw IOException("Could not save APK")
            destination
        } finally {
            partial.delete()
        }
    }

    companion object {
        const val REPOSITORY_URL = "https://github.com/MrKot-Anon/MD3-Agent"
        private const val API_URL = "https://api.github.com/repos/MrKot-Anon/MD3-Agent/releases/latest"
        private const val MAX_APK_BYTES = 250L * 1024 * 1024
    }
}

internal object UpdateHostPolicy {
    fun permits(host: String, path: String): Boolean = when (host) {
        "api.github.com" -> path == "/repos/MrKot-Anon/MD3-Agent/releases/latest"
        "github.com" -> path.startsWith("/MrKot-Anon/MD3-Agent/releases/download/") && path.endsWith(".apk")
        "release-assets.githubusercontent.com", "objects.githubusercontent.com" -> true
        else -> false
    }
}
