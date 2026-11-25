package dev.huecoder.md3agent.net

import dev.huecoder.md3agent.data.settings.NetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpClientProvider(
    networkSettings: Flow<NetworkSettings>,
    scope: CoroutineScope,
    private val permits: suspend (HttpUrl) -> Boolean = { false },
) {
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            if (!runBlocking { permits(url) }) {
                throw IOException("Network access is limited to configured model endpoints")
            }
            chain.proceed(chain.request())
        })
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    val client: StateFlow<OkHttpClient> = networkSettings
        .map { baseClient }
        .stateIn(scope, SharingStarted.Eagerly, baseClient)

    fun current(): OkHttpClient = client.value

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 20L
        const val READ_TIMEOUT_SECONDS = 180L
        const val WRITE_TIMEOUT_SECONDS = 60L
    }
}

internal object LocalEndpointPolicy {
    fun permits(url: HttpUrl, configuredBaseUrls: List<String>): Boolean =
        configuredBaseUrls.any { raw ->
            val base = raw.trim().toHttpUrlOrNull() ?: return@any false
            if (!permitsBaseUrl(base)) return@any false
            val path = base.encodedPath.trimEnd('/').ifEmpty { "/" }
            url.scheme == base.scheme && url.host == base.host && url.port == base.port &&
                url.username.isEmpty() && url.password.isEmpty() &&
                (url.encodedPath == path || url.encodedPath.startsWith(if (path == "/") path else "$path/"))
        }

    fun permitsBaseUrl(url: HttpUrl): Boolean =
        url.scheme in setOf("http", "https") && url.username.isEmpty() && url.password.isEmpty() &&
            url.query == null && url.fragment == null
}
