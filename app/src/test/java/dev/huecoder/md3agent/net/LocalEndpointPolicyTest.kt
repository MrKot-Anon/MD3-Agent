package dev.huecoder.md3agent.net

import dev.huecoder.md3agent.data.settings.ProviderProfile
import dev.huecoder.md3agent.llm.ProviderEndpoint
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalEndpointPolicyTest {
    @Test fun acceptsConfiguredEndpointWithNestedPath() {
        val bases = listOf("https://model.example/private-token/v1")
        assertTrue(LocalEndpointPolicy.permitsBaseUrl(bases.single().toHttpUrl()))
        assertTrue(LocalEndpointPolicy.permits("https://model.example/private-token/v1/models".toHttpUrl(), bases))
        assertTrue(LocalEndpointPolicy.permits("https://model.example/private-token/v1/chat/completions".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("https://model.example/private-token/v11/models".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("https://model.example/v1/models".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("http://model.example/private-token/v1/models".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("https://other.example/private-token/v1/models".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("https://model.example:8443/private-token/v1/models".toHttpUrl(), bases))
        assertFalse(LocalEndpointPolicy.permits("https://api.openai.com/v1/models".toHttpUrl(), bases))
    }

    @Test fun rejectsCredentialsAndParametersInBaseUrl() {
        assertFalse(LocalEndpointPolicy.permitsBaseUrl("https://user:pass@model.example/v1".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permitsBaseUrl("https://model.example/v1?key=secret".toHttpUrl()))
        assertFalse(LocalEndpointPolicy.permitsBaseUrl("https://model.example/v1#fragment".toHttpUrl()))
    }

    @Test fun providerBuildsRequestUnderNestedBasePath() {
        val base = "https://model.example/private-token/v1"
        val endpoint = ProviderEndpoint(ProviderProfile("test", "Test", base))
        val url = endpoint.url("models")
        assertTrue(url.toString() == "$base/models")
        assertTrue(LocalEndpointPolicy.permits(url, listOf(base)))
    }
}
