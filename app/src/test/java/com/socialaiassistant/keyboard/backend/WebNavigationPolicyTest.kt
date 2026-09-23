package com.socialaiassistant.keyboard.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WebNavigationPolicyTest {
    @Test
    fun exact_backend_https_origin_is_allowed_in_webview() {
        assertEquals(
            WebNavigationPolicy.Decision.ALLOW_TRUSTED,
            WebNavigationPolicy.classify("${BackendConfig.API_BASE}/account/index.html?tab=payments", false)
        )
    }

    @Test
    fun backend_lookalike_and_userinfo_urls_are_not_trusted() {
        val host = java.net.URI(BackendConfig.API_BASE).host
        assertEquals(
            WebNavigationPolicy.Decision.OPEN_EXTERNAL,
            WebNavigationPolicy.classify("https://$host.evil.example/account/", false)
        )
        assertEquals(
            WebNavigationPolicy.Decision.BLOCK,
            WebNavigationPolicy.classify("https://$host@evil.example/account/", false)
        )
    }

    @Test
    fun cleartext_and_active_content_schemes_are_blocked() {
        listOf(
            "http://example.com/",
            "javascript:alert(1)",
            "data:text/html,pwn",
            "file:///sdcard/token.txt",
            "content://com.example/private",
            "intent://pay#Intent;scheme=https;end"
        ).forEach { url ->
            assertEquals(url, WebNavigationPolicy.Decision.BLOCK, WebNavigationPolicy.classify(url, false))
        }
    }

    @Test
    fun external_https_mail_and_tel_are_handed_off() {
        listOf(
            "https://payments.example/checkout",
            "mailto:support@example.com",
            "tel:+8801000000000"
        ).forEach { url ->
            assertEquals(url, WebNavigationPolicy.Decision.OPEN_EXTERNAL, WebNavigationPolicy.classify(url, false))
        }
    }

    @Test
    fun auth_callback_is_exact_and_auth_only() {
        assertEquals(
            WebNavigationPolicy.Decision.AUTH_CALLBACK,
            WebNavigationPolicy.classify("${BackendConfig.ANDROID_AUTH_CALLBACK}?state=s&code=c", true)
        )
        assertEquals(
            WebNavigationPolicy.Decision.BLOCK,
            WebNavigationPolicy.classify("${BackendConfig.ANDROID_AUTH_CALLBACK}?state=s&code=c", false)
        )
        assertTrue(WebNavigationPolicy.isExactAuthCallback("${BackendConfig.ANDROID_AUTH_CALLBACK}?state=s&code=c"))
        assertFalse(WebNavigationPolicy.isExactAuthCallback("${BackendConfig.ANDROID_AUTH_CALLBACK}/extra?state=s&code=c"))
        assertFalse(WebNavigationPolicy.isExactAuthCallback("${BackendConfig.ANDROID_AUTH_CALLBACK}#fragment"))
    }

    @Test
    fun alternate_ports_are_not_same_origin_or_callback() {
        val backend = java.net.URI(BackendConfig.API_BASE)
        val callback = java.net.URI(BackendConfig.ANDROID_AUTH_CALLBACK)
        assertEquals(
            WebNavigationPolicy.Decision.BLOCK,
            WebNavigationPolicy.classify("https://${backend.host}:444/account/", false)
        )
        assertEquals(
            WebNavigationPolicy.Decision.BLOCK,
            WebNavigationPolicy.classify("https://${callback.host}:444/auth?state=s&code=c", true)
        )
    }
}
