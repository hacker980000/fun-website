package com.socialaiassistant.keyboard.ai

import kotlinx.serialization.json.JsonObject

interface AiGateway {
    suspend fun generate(request: AiGenerationRequest): AiGenerationResult
}

data class AiGenerationRequest(
    val prompt: PromptBundle,
    val modelMode: ModelMode = ModelMode.FAST,
    val includeConversationMemory: Boolean = false,
    val managedPayload: JsonObject? = null
)

data class AiGenerationResult(
    val rawText: String,
    val model: String
)

sealed class AiGatewayException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {
    class InvalidApiKey : AiGatewayException("OpenRouter API key is missing or invalid.")
    class RateLimited : AiGatewayException("OpenRouter rate limit reached. Please try again shortly.")
    class InsufficientCredits : AiGatewayException("OpenRouter account has insufficient credits.")
    class Timeout(cause: Throwable? = null) : AiGatewayException("AI request timed out.", cause)
    class Offline(cause: Throwable? = null) : AiGatewayException("No network connection is available.", cause)
    class ProviderFailure(cause: Throwable? = null) : AiGatewayException("AI provider is temporarily unavailable.", cause)
    class MalformedResponse : AiGatewayException("AI provider returned an unreadable response.")
}

object OpenRouterModels {
    const val FAST_PRIMARY = "google/gemini-2.5-flash-lite"
    const val FAST_FALLBACK = "google/gemini-2.5-flash"
    const val SMART_PRIMARY = "google/gemini-2.5-flash"
    const val SMART_FALLBACK = "google/gemini-2.5-flash-lite"

    fun ordered(mode: ModelMode): List<String> = when (mode) {
        ModelMode.FAST -> listOf(FAST_PRIMARY, FAST_FALLBACK)
        ModelMode.SMART -> listOf(SMART_PRIMARY, SMART_FALLBACK)
    }
}
