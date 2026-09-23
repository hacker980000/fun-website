package com.socialaiassistant.keyboard.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/** Creates the single production HTTP client used by managed backend + OpenRouter. */
object SecureHttpClientFactory {
    private const val CONNECT_TIMEOUT_SECONDS = 8L
    private const val READ_TIMEOUT_SECONDS = 20L
    private const val WRITE_TIMEOUT_SECONDS = 15L
    private const val CALL_TIMEOUT_SECONDS = 26L

    fun create(): OkHttpClient = OkHttpClient.Builder()
        // TLS only; the interceptor below also enforces HTTPS + exact host + port 443.
        .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS))
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        // AI generation and account mutations are POST requests. Never replay them
        // automatically after a transport failure; callers decide whether to retry.
        .retryOnConnectionFailure(false)
        // Do not follow a server-controlled Location to another origin. The app uses
        // fixed API endpoints and treats redirects as provider/backend failures.
        .followRedirects(false)
        .followSslRedirects(false)
        .addInterceptor(Interceptor { chain ->
            val url = chain.request().url
            if (!NetworkEndpointPolicy.allows(
                    scheme = url.scheme,
                    host = url.host,
                    port = url.port,
                    username = url.username,
                    password = url.password
                )
            ) {
                throw IOException("Blocked network destination")
            }
            chain.proceed(chain.request())
        })
        .build()
}
