package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.backend.BackendException
import com.socialaiassistant.keyboard.backend.ManagedSessionStore
import com.socialaiassistant.keyboard.backend.SocialAiBackendClient
import com.socialaiassistant.keyboard.settings.AppSettings

class GatewayRouter(
    private val settingsProvider: suspend () -> AppSettings,
    private val personalGateway: AiGateway,
    private val personalApiConfigured: () -> Boolean,
    private val managedClient: SocialAiBackendClient,
    private val managedSessionStore: ManagedSessionStore
) : AiGateway {
    override suspend fun generate(request: AiGenerationRequest): AiGenerationResult {
        val settings = settingsProvider()
        if (settings.gatewayMode == GatewayMode.PERSONAL) {
            return personalGateway.generate(request)
        }

        val payload = request.managedPayload
        if (payload == null) {
            if (personalApiConfigured()) return personalGateway.generate(request)
            throw BackendException(
                "MANAGED_FEATURE_UNAVAILABLE",
                "Rewrite, Translate and Grammar Fix currently use the optional Personal OpenRouter mode. Add a personal key or switch to a managed social/caption action."
            )
        }
        if (!managedSessionStore.isLoggedIn()) {
            throw BackendException("AUTH_REQUIRED", "AI ব্যবহার করতে আগে Login করুন।", 401)
        }
        val result = managedClient.generate(payload)
        return AiGenerationResult(rawText = result.reply, model = result.model)
    }
}
