package dev.huecoder.md3agent.data.updates

import dev.huecoder.md3agent.net.HttpClientProvider

sealed interface UpdateStatus {
    data class UpToDate(val latest: String) : UpdateStatus
    data class Available(val version: String, val pageUrl: String, val apkUrl: String?) : UpdateStatus
    data object NotPublished : UpdateStatus
    data object Unreachable : UpdateStatus
}

class UpdateChecker(@Suppress("UNUSED_PARAMETER") http: HttpClientProvider, @Suppress("UNUSED_PARAMETER") installedVersion: String) {
    suspend fun check(): UpdateStatus = UpdateStatus.Unreachable

    companion object {
        const val REPOSITORY_URL = "https://github.com/MrKot-Anon/MD3-Agent"
    }
}
