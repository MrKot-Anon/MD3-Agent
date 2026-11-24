package dev.huecoder.md3agent.net

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEndpointPolicyTest {
    @Test fun permitsOnlyLocalModelApi() {
        assertTrue(LocalEndpointPolicy.permits("http://172.16.88.10:8080/v1/chat/completions".toHttpUrl()))
        assertTrue(LocalEndpointPolicy.permits("http://172.16.88.10:8080/v1/models".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permits("http://172.16.88.10:8081/v1/models".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permits("https://172.16.88.10:8080/v1/models".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permits("http://172.16.88.10:8080/admin".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permits("https://api.github.com/v1/models".toHttpUrl()))
    }
}
