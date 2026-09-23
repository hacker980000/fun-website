package com.socialaiassistant.keyboard.ai

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OpenRouterGatewayTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fast_mode_uses_flash_lite_first_and_returns_content() = runTest {
        server.enqueue(jsonResponse("hello from model"))
        val gateway = gateway(ModelMode.FAST)

        val result = gateway.generate(request(ModelMode.FAST))

        assertEquals("hello from model", result.rawText)
        assertEquals(OpenRouterModels.FAST_PRIMARY, result.model)
        val recorded = server.takeRequest()
        assertEquals("Bearer sk-or-test", recorded.getHeader("Authorization"))
        assertTrue(recorded.body.readUtf8().contains("\"model\":\"${OpenRouterModels.FAST_PRIMARY}\""))
    }

    @Test
    fun retryable_5xx_falls_back_to_second_model() = runTest {
        server.enqueue(MockResponse().setResponseCode(503).setBody("provider unavailable"))
        server.enqueue(jsonResponse("fallback reply"))
        val gateway = gateway(ModelMode.FAST)

        val result = gateway.generate(request(ModelMode.FAST))

        assertEquals("fallback reply", result.rawText)
        assertEquals(OpenRouterModels.FAST_FALLBACK, result.model)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun invalid_key_is_normalized_and_not_retried() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("invalid key"))
        val gateway = gateway(ModelMode.FAST)

        val error = captureGatewayError { gateway.generate(request(ModelMode.FAST)) }

        assertTrue(error is AiGatewayException.InvalidApiKey)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun rate_limit_is_normalized_and_not_retried() = runTest {
        server.enqueue(MockResponse().setResponseCode(429).setBody("rate limited"))
        val gateway = gateway(ModelMode.SMART)

        val error = captureGatewayError { gateway.generate(request(ModelMode.SMART)) }

        assertTrue(error is AiGatewayException.RateLimited)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun malformed_success_body_is_rejected() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"choices\":[]}"))
        val gateway = gateway(ModelMode.FAST)

        val error = captureGatewayError { gateway.generate(request(ModelMode.FAST)) }

        assertTrue(error is AiGatewayException.MalformedResponse)
    }

    @Test
    fun total_timeout_is_normalized() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val gateway = gateway(ModelMode.FAST, timeoutMs = 120)

        val error = captureGatewayError { gateway.generate(request(ModelMode.FAST)) }

        assertTrue(error is AiGatewayException.Timeout)
    }

    @Test
    fun missing_key_is_invalid_api_key_without_network_call() = runTest {
        val gateway = OpenRouterGateway(
            client = OkHttpClient(),
            apiKeyProvider = { null },
            endpoint = server.url("/api/v1/chat/completions"),
            totalTimeoutMs = 1_000
        )

        val error = captureGatewayError { gateway.generate(request(ModelMode.FAST)) }

        assertTrue(error is AiGatewayException.InvalidApiKey)
        assertEquals(0, server.requestCount)
    }

    private fun gateway(mode: ModelMode, timeoutMs: Long = 2_000): OpenRouterGateway {
        assertNotNull(mode)
        return OpenRouterGateway(
            client = OkHttpClient.Builder()
                .readTimeout(1, TimeUnit.SECONDS)
                .build(),
            apiKeyProvider = { "sk-or-test" },
            endpoint = server.url("/api/v1/chat/completions"),
            totalTimeoutMs = timeoutMs
        )
    }

    private fun request(mode: ModelMode) = AiGenerationRequest(
        prompt = PromptBundle(system = "system rules", user = "user context"),
        modelMode = mode,
        includeConversationMemory = false
    )

    private fun jsonResponse(content: String): MockResponse = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"choices":[{"message":{"content":"$content"}}]}""")

    private suspend fun captureGatewayError(block: suspend () -> Unit): AiGatewayException {
        return try {
            block()
            throw AssertionError("Expected AiGatewayException")
        } catch (error: AiGatewayException) {
            error
        }
    }
}
