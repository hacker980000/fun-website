package com.socialaiassistant.keyboard.backend

import java.net.URI

/**
 * Strict main-frame navigation policy for the hosted account/auth WebViews.
 *
 * The embedded WebViews may render only the configured HTTPS backend origin.
 * External HTTPS/mail/tel links are handed to another app instead of inheriting
 * this WebView's first-party cookies or DOM storage. Unsafe/custom schemes are
 * blocked. The Chromium HTTPS auth callback is intercepted only by AuthActivity.
 */
object WebNavigationPolicy {
    enum class Decision {
        ALLOW_TRUSTED,
        AUTH_CALLBACK,
        OPEN_EXTERNAL,
        BLOCK
    }

    private val trustedOrigin = strictUri(BackendConfig.API_BASE)
        ?: error("BackendConfig.API_BASE must be an absolute HTTPS URL")
    private val authCallback = strictUri(BackendConfig.ANDROID_AUTH_CALLBACK)
        ?: error("BackendConfig.ANDROID_AUTH_CALLBACK must be an absolute HTTPS URL")

    fun classify(rawUrl: String?, allowAuthCallback: Boolean): Decision {
        val uri = strictUri(rawUrl) ?: return Decision.BLOCK

        if (sameEndpoint(uri, authCallback)) {
            return if (allowAuthCallback) Decision.AUTH_CALLBACK else Decision.BLOCK
        }
        if (sameHttpsOrigin(uri, trustedOrigin)) return Decision.ALLOW_TRUSTED

        // Same-host URL variants with the wrong port/path must not be handed to
        // another app as if they were an unrelated external destination.
        if (sameHost(uri, trustedOrigin) || sameHost(uri, authCallback)) return Decision.BLOCK

        return when (uri.scheme.lowercase()) {
            "https" -> Decision.OPEN_EXTERNAL
            "mailto", "tel" -> Decision.OPEN_EXTERNAL
            else -> Decision.BLOCK
        }
    }

    fun isExactAuthCallback(rawUrl: String?): Boolean {
        val uri = strictUri(rawUrl) ?: return false
        return sameEndpoint(uri, authCallback)
    }

    private fun strictUri(rawUrl: String?): URI? {
        val raw = rawUrl?.trim().orEmpty()
        if (raw.isEmpty() || raw.length > MAX_URL_LENGTH) return null
        if (raw.any { it == '\\' || it.code < 0x20 || it.code == 0x7f }) return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (!uri.isAbsolute || uri.isOpaque && scheme !in setOf("mailto", "tel")) return null
        if (uri.userInfo != null) return null
        return uri
    }

    private fun sameHttpsOrigin(candidate: URI, expected: URI): Boolean {
        if (!candidate.scheme.equals("https", ignoreCase = true)) return false
        if (!expected.scheme.equals("https", ignoreCase = true)) return false
        val candidateHost = candidate.host ?: return false
        val expectedHost = expected.host ?: return false
        if (!candidateHost.equals(expectedHost, ignoreCase = true)) return false
        return normalizedHttpsPort(candidate.port) == normalizedHttpsPort(expected.port)
    }

    private fun sameHost(candidate: URI, expected: URI): Boolean {
        val candidateHost = candidate.host ?: return false
        val expectedHost = expected.host ?: return false
        return candidateHost.equals(expectedHost, ignoreCase = true)
    }

    private fun sameEndpoint(candidate: URI, expected: URI): Boolean {
        if (!sameHttpsOrigin(candidate, expected)) return false
        val candidatePath = candidate.rawPath.orEmpty().ifEmpty { "/" }
        val expectedPath = expected.rawPath.orEmpty().ifEmpty { "/" }
        return candidatePath == expectedPath && candidate.fragment == null
    }

    private fun normalizedHttpsPort(port: Int): Int = if (port == -1) 443 else port

    private const val MAX_URL_LENGTH = 4096
}
