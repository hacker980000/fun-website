package com.socialaiassistant.keyboard.network

import com.socialaiassistant.keyboard.backend.BackendConfig
import java.net.URI
import java.util.Locale

/**
 * Production egress policy for native HTTP calls.
 *
 * WebView navigation has its own policy. This policy protects bearer tokens,
 * device proof headers, and AI prompts sent through OkHttp.
 */
object NetworkEndpointPolicy {
    private const val OPENROUTER_HOST = "openrouter.ai"
    private const val HTTPS_PORT = 443

    private val backendHost: String = requireNotNull(URI(BackendConfig.API_BASE).host) {
        "BackendConfig.API_BASE must contain an HTTPS host"
    }.lowercase(Locale.ROOT)

    val allowedHosts: Set<String> = setOf(backendHost, OPENROUTER_HOST)

    fun allows(
        scheme: String,
        host: String,
        port: Int,
        username: String = "",
        password: String = ""
    ): Boolean {
        if (!scheme.equals("https", ignoreCase = true)) return false
        if (username.isNotEmpty() || password.isNotEmpty()) return false
        if (port != HTTPS_PORT) return false
        return host.lowercase(Locale.ROOT) in allowedHosts
    }
}
