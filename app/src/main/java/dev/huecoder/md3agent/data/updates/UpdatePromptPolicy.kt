package dev.huecoder.md3agent.data.updates

import kotlin.time.Duration.Companion.hours

object UpdatePromptPolicy {
    private val CHECK_INTERVAL_MS = 12.hours.inWholeMilliseconds

    fun shouldCheck(now: Long, lastCheckAt: Long): Boolean = now - lastCheckAt >= CHECK_INTERVAL_MS

    fun offerFor(status: UpdateStatus, dismissedVersion: String, dismissedAt: Long = 0, now: Long = System.currentTimeMillis()): UpdateStatus.Available? =
        (status as? UpdateStatus.Available)?.takeIf { it.version != dismissedVersion || now - dismissedAt >= 24.hours.inWholeMilliseconds }
}
