package dev.huecoder.md3agent.data.updates

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateHostPolicyTest {
    @Test fun onlyThisForkReleaseHostsAreAllowed() {
        assertTrue(UpdateHostPolicy.permits("api.github.com", "/repos/MrKot-Anon/MD3-Agent/releases/latest"))
        assertTrue(UpdateHostPolicy.permits("github.com", "/MrKot-Anon/MD3-Agent/releases/download/v0.9.6-local.4/app-release.apk"))
        assertTrue(UpdateHostPolicy.permits("release-assets.githubusercontent.com", "/github-production-release-asset/123"))
        assertFalse(UpdateHostPolicy.permits("api.github.com", "/repos/huecoder/MD3-Agent/releases/latest"))
        assertFalse(UpdateHostPolicy.permits("github.com", "/huecoder/MD3-Agent/releases/download/v1/app.apk"))
        assertFalse(UpdateHostPolicy.permits("example.com", "/apk"))
    }
}
