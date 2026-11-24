package dev.huecoder.md3agent.net

import dev.huecoder.md3agent.data.settings.NetworkSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.HttpUrl
import java.io.IOException
import java.util.concurrent.TimeUnit

class HttpClientProvider(
    networkSettings: Flow<NetworkSettings>,
    scope: CoroutineScope,
    private val permits: (HttpUrl) -> Boolean = LocalEndpointPolicy::permits,
) {
    private val baseClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            if (!permits(url)) {
                throw IOException("Network access is limited to the configured local model endpoint")
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
    fun permits(url: HttpUrl): Boolean =
        url.scheme == "http" && url.host == "172.16.88.10" && url.port == 8080 && url.encodedPath.startsWith("/v1/")
}
