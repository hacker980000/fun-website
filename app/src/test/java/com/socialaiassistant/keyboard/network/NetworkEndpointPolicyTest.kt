package com.socialaiassistant.keyboard.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkEndpointPolicyTest {
    private val backend = NetworkEndpointPolicy.allowedHosts.first { it.endsWith("workers.dev") }

    @Test
    fun approved_https_origins_on_port_443_are_allowed() {
        assertTrue(NetworkEndpointPolicy.allows("https", backend, 443))
        assertTrue(NetworkEndpointPolicy.allows("HTTPS", "OPENROUTER.AI", 443))
    }

    @Test
    fun cleartext_alt_ports_lookalikes_and_userinfo_are_blocked() {
        assertFalse(NetworkEndpointPolicy.allows("http", backend, 80))
        assertFalse(NetworkEndpointPolicy.allows("https", backend, 8443))
        assertFalse(NetworkEndpointPolicy.allows("https", "evil.$backend", 443))
        assertFalse(NetworkEndpointPolicy.allows("https", "$backend.evil.example", 443))
        assertFalse(NetworkEndpointPolicy.allows("https", backend, 443, username = "token"))
        assertFalse(NetworkEndpointPolicy.allows("https", backend, 443, password = "secret"))
        assertFalse(NetworkEndpointPolicy.allows("https", "example.com", 443))
    }
}
