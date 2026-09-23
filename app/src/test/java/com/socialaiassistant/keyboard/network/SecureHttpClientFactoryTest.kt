package com.socialaiassistant.keyboard.network

import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureHttpClientFactoryTest {
    @Test
    fun production_client_is_tls_only_bounded_and_non_replaying() {
        val client = SecureHttpClientFactory.create()

        assertEquals(listOf(ConnectionSpec.MODERN_TLS), client.connectionSpecs)
        assertEquals(TimeUnit.SECONDS.toMillis(8), client.connectTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(20), client.readTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(15), client.writeTimeoutMillis.toLong())
        assertEquals(TimeUnit.SECONDS.toMillis(26), client.callTimeoutMillis.toLong())
        assertFalse(client.retryOnConnectionFailure)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertTrue(client.interceptors.isNotEmpty())
    }
}
