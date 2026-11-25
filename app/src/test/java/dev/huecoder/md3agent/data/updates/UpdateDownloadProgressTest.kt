package dev.huecoder.md3agent.data.updates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateDownloadProgressTest {
    @Test fun percentAndEtaUseKnownFileSize() {
        val progress = UpdateDownloadProgress(bytesRead = 25, totalBytes = 100, elapsedMillis = 2000)
        assertEquals(25, progress.percent)
        assertEquals(6L, progress.etaSeconds)
        assertEquals(100, UpdateDownloadProgress(100, 100, 3000).percent)
        assertEquals(0L, UpdateDownloadProgress(100, 100, 3000).etaSeconds)
    }

    @Test fun unknownSizeOrEarlyTransferHasNoEta() {
        assertNull(UpdateDownloadProgress(20, null, 2000).percent)
        assertNull(UpdateDownloadProgress(20, null, 2000).etaSeconds)
        assertNull(UpdateDownloadProgress(20, 100, 500).etaSeconds)
    }
}
