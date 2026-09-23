package com.socialaiassistant.keyboard.ai

import com.socialaiassistant.keyboard.crypto.SecretStore
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

class OpenRouterGateway(
    private val client: OkHttpClient,
    private val apiKeyProvider: () -> String?,
    private val endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
    private val totalTimeoutMs: Long = TOTAL_TIMEOUT_MS
) : AiGateway {
    constructor(
        client: OkHttpClient,
        secretStore: SecretStore,
        endpoint: HttpUrl = DEFAULT_ENDPOINT.toHttpUrl(),
        totalTimeoutMs: Long = TOTAL_TIMEOUT_MS
    ) : this(client, secretStore::getOpenRouterKey, endpoint, totalTimeoutMs)

    override suspend fun generate(request: AiGenerationRequest): AiGenerationResult {
        val apiKey = apiKeyProvider()?.trim().orEmpty()
        if (apiKey.isEmpty()) throw AiGatewayException.InvalidApiKey()

        return try {
            withTimeout(totalTimeoutMs) {
                generateWithFallback(apiKey, request)
            }
        } catch (error: TimeoutCancellationException) {
            throw AiGatewayException.Timeout(error)
        } catch (error: SocketTimeoutException) {
            throw AiGatewayException.Timeout(error)
        } catch (error: UnknownHostException) {
            throw AiGatewayException.Offline(error)
        } catch (error: ConnectException) {
            throw AiGatewayException.Offline(error)
        } catch (error: CancellationException) {
            throw error
        } catch (error: AiGatewayException) {
            throw error
        } catch (error: IOException) {
            throw AiGatewayException.ProviderFailure(error)
        }
    }

    private suspend fun generateWithFallback(
        apiKey: String,
        generation: AiGenerationRequest
    ): AiGenerationResult {
        val models = OpenRouterModels.ordered(generation.modelMode)
        var retryableFailure: Throwable? = null

        for ((index, model) in models.withIndex()) {
            try {
                return callModel(apiKey, model, generation)
            } catch (error: RetryableProviderFailure) {
                retryableFailure = error
                if (index == models.lastIndex) break
            }
        }
        throw AiGatewayException.ProviderFailure(retryableFailure)
    }

    private suspend fun callModel(
        apiKey: String,
        model: String,
        generation: AiGenerationRequest
    ): AiGenerationResult {
        val requestBody = buildRequestJson(model, generation)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .header("Cache-Control", "no-store")
            .post(requestBody)
            .build()

        val response = client.newCall(request).await()
        response.use { httpResponse ->
            val body = httpResponse.body?.string().orEmpty()
            when (httpResponse.code) {
                200 -> return parseSuccess(body, model)
                401 -> throw AiGatewayException.InvalidApiKey()
                402 -> throw AiGatewayException.InsufficientCredits()
                429 -> throw AiGatewayException.RateLimited()
                in 500..599 -> throw RetryableProviderFailure(httpResponse.code)
                else -> throw AiGatewayException.ProviderFailure()
            }
        }
    }

    private fun buildRequestJson(
        model: String,
        generation: AiGenerationRequest
    ) = buildJsonObject {
        put("model", model)
        put("messages", buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", generation.prompt.system)
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", generation.prompt.user)
            })
        })
        put("temperature", 0.72)
        put("max_tokens", if (generation.includeConversationMemory) 440 else 240)
    }

    private fun parseSuccess(body: String, model: String): AiGenerationResult {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: throw AiGatewayException.MalformedResponse()
        val content = runCatching {
            root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
                ?.get("content")
                ?.jsonPrimitive
                ?.content
        }.getOrNull()?.trim().orEmpty()
        if (content.isEmpty()) throw AiGatewayException.MalformedResponse()
        return AiGenerationResult(rawText = content, model = model)
    }

    private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) {
                if (continuation.isActive) continuation.resumeWithException(error)
            }

            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) {
                    continuation.resume(response)
                } else {
                    response.close()
                }
            }
        })
    }

    private class RetryableProviderFailure(statusCode: Int) :
        IOException("Retryable OpenRouter provider failure: HTTP $statusCode")

    private companion object {
        const val DEFAULT_ENDPOINT = "https://openrouter.ai/api/v1/chat/completions"
        const val TOTAL_TIMEOUT_MS = 22_000L
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}
