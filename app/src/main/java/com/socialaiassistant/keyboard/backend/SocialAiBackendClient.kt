package com.socialaiassistant.keyboard.backend

import com.socialaiassistant.keyboard.ai.AiGatewayException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class SocialAiBackendClient(
    private val client: OkHttpClient,
    private val sessionStore: ManagedSessionStore,
    private val installationIdentity: InstallationIdentity,
    private val deviceProof: DeviceProofManager,
    private val totalTimeoutMs: Long = 24_000L,
    private val apiBase: String = BackendConfig.API_BASE,
    private val requestSigner: (String, String, Long, String, String) -> String = deviceProof::signRequest
) {
    suspend fun exchangeAuth(code: String): ManagedSession {
        val body = buildJsonObject {
            put("code", code)
            put("installationId", installationIdentity.getOrCreate())
            put("extensionVersion", BackendConfig.APP_VERSION)
            put("devicePublicKeyJwk", Json.parseToJsonElement(deviceProof.publicJwkJson()))
        }
        val root = request("/api/v1/auth/extension-exchange", "POST", body, auth = false)
        val user = root["user"]?.jsonObject ?: JsonObject(emptyMap())
        val session = ManagedSession(
            token = root.string("sessionToken"),
            expiresAt = root.string("expiresAt"),
            userId = user.string("id"),
            email = user.string("email"),
            mobile = user.string("mobile")
        )
        if (session.token.isBlank() || session.userId.isBlank()) throw BackendException("INVALID_RESPONSE", "Login response was incomplete.")
        sessionStore.save(session)
        return session
    }

    suspend fun logout() {
        try { request("/api/v1/auth/logout", "POST", buildJsonObject {}, auth = true) }
        finally { sessionStore.clear() }
    }

    suspend fun accountState(): ManagedAccountState {
        val me = request("/api/v1/auth/me", "GET", null, auth = true)
        val usage = request("/api/v1/usage", "GET", null, auth = true)
        val user = me["user"]?.jsonObject ?: JsonObject(emptyMap())
        val sub = me["subscription"]?.jsonObject ?: JsonObject(emptyMap())
        val release = me["release"]?.jsonObject ?: JsonObject(emptyMap())
        val limits = usage["limits"]?.jsonObject ?: JsonObject(emptyMap())
        val daily = usage["daily"]?.jsonObject ?: JsonObject(emptyMap())
        val cycle = usage["cycle"]?.jsonObject ?: JsonObject(emptyMap())
        val legacyStatus = sub.string("effectiveStatus").ifBlank { sub.string("status").ifBlank { "INACTIVE" } }
        val legacyExpiry = sub["cycle_expires_at"]?.jsonPrimitive?.contentOrNull
            ?: sub["expires_at"]?.jsonPrimitive?.contentOrNull
            ?: sub["endsAt"]?.jsonPrimitive?.contentOrNull
        val entitlements = parseEntitlements(me, legacyStatus, legacyExpiry)
        return ManagedAccountState(
            userId = user.string("id"),
            email = user.string("email"),
            mobile = user.string("mobile"),
            subscriptionStatus = legacyStatus,
            subscriptionEndsAt = legacyExpiry,
            entitlements = entitlements,
            passwordResetRequired = user["passwordResetRequired"]?.jsonPrimitive?.booleanOrNull ?: false,
            dailyUsed = daily.int("request_count"),
            dailyLimit = limits.int("daily").takeIf { it > 0 } ?: 200,
            cycleUsed = cycle.int("request_count"),
            cycleLimit = limits.int("cycle").takeIf { it > 0 } ?: 4000,
            currentVersion = release.string("currentVersion").ifBlank { BackendConfig.APP_VERSION },
            minimumVersion = release.string("minimumVersion").ifBlank { "35.0.0" }
        )
    }

    suspend fun generate(payload: JsonObject): ManagedAiResult {
        val session = sessionStore.load() ?: throw BackendException("AUTH_REQUIRED", "Login required.", 401)
        val path = "/api/v1/ai/generate"
        val bodyText = payload.toString()
        val requestId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()
        val signature = requestSigner("POST", path, timestamp, requestId, bodyText)
        val headers = mapOf(
            "Authorization" to "Bearer ${session.token}",
            "X-Device-ID" to installationIdentity.getOrCreate(),
            "X-Extension-Version" to BackendConfig.APP_VERSION,
            "X-Request-ID" to requestId,
            "X-Device-Proof-Version" to "1",
            "X-Device-Proof-Timestamp" to timestamp.toString(),
            "X-Device-Proof-Signature" to signature
        )
        val root = request(
            path = path,
            method = "POST",
            body = null,
            auth = false,
            bodyText = bodyText,
            extraHeaders = headers,
            productSensitive = true
        )
        val reply = root.string("reply").trim()
        if (reply.isEmpty()) throw AiGatewayException.MalformedResponse()
        return ManagedAiResult(
            reply = reply,
            model = root.string("model").ifBlank { "managed" },
            dailyUsed = root["usage"]?.jsonObject?.int("daily") ?: 0,
            cycleUsed = root["usage"]?.jsonObject?.int("cycle") ?: 0
        )
    }

    suspend fun notifications(): JsonObject = request("/api/v1/notifications", "GET", null, auth = true)
    suspend fun deviceStatus(): JsonObject = request("/api/v1/device", "GET", null, auth = true)
    suspend fun payments(): JsonObject = request("/api/v1/payments", "GET", null, auth = true)
    suspend fun activity(): JsonObject = request("/api/v1/account/activity", "GET", null, auth = true)

    private suspend fun request(
        path: String,
        method: String,
        body: JsonObject?,
        auth: Boolean,
        bodyText: String? = null,
        extraHeaders: Map<String, String> = emptyMap(),
        productSensitive: Boolean = auth
    ): JsonObject {
        return try {
            withTimeout(totalTimeoutMs) {
                val finalBodyText = bodyText ?: body?.toString()
                val builder = Request.Builder()
                    .url(apiBase + path)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("X-Extension-Channel", BackendConfig.RELEASE_CHANNEL)
                    .header("X-Extension-Version", BackendConfig.APP_VERSION)
                    .header("Cache-Control", "no-store")
                if (productSensitive) builder.header(PRODUCT_HEADER, BackendConfig.PRODUCT_CODE)
                if (auth) {
                    val session = sessionStore.load() ?: throw BackendException("AUTH_REQUIRED", "Login required.", 401)
                    builder.header("Authorization", "Bearer ${session.token}")
                }
                extraHeaders.forEach { (key, value) -> builder.header(key, value) }
                when (method.uppercase()) {
                    "GET" -> builder.get()
                    "POST" -> builder.post((finalBodyText ?: "{}").toRequestBody(JSON_MEDIA_TYPE))
                    else -> error("Unsupported HTTP method")
                }
                client.newCall(builder.build()).await().use { response -> parseResponse(response) }
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
        } catch (error: BackendException) {
            throw error
        } catch (error: AiGatewayException) {
            throw error
        } catch (error: IOException) {
            throw AiGatewayException.ProviderFailure(error)
        }
    }


    private fun parseEntitlements(
        me: JsonObject,
        legacyStatus: String,
        legacyExpiry: String?
    ): List<ProductEntitlement> {
        if (!me.containsKey("entitlements")) {
            return listOf(
                ProductEntitlement(ManagedAccountState.PRODUCT_KEYBOARD, legacyStatus, legacyExpiry),
                ProductEntitlement(ManagedAccountState.PRODUCT_ASSISTANT_PRO, legacyStatus, legacyExpiry)
            )
        }
        val items = me["entitlements"] as? JsonArray ?: return emptyList()
        return items.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val code = item.string("productCode").ifBlank { item.string("product_code") }.uppercase()
            if (code != ManagedAccountState.PRODUCT_KEYBOARD && code != ManagedAccountState.PRODUCT_ASSISTANT_PRO) {
                return@mapNotNull null
            }
            val status = item.string("status").ifBlank { "INACTIVE" }.uppercase()
            val expiry = item["cycle_expires_at"]?.jsonPrimitive?.contentOrNull
                ?: item["cycleExpiresAt"]?.jsonPrimitive?.contentOrNull
                ?: item["expires_at"]?.jsonPrimitive?.contentOrNull
                ?: item["endsAt"]?.jsonPrimitive?.contentOrNull
            ProductEntitlement(code, status, expiry)
        }
    }

    private fun parseResponse(response: Response): JsonObject {
        val raw = response.body?.string().orEmpty()
        val root = runCatching { Json.parseToJsonElement(raw).jsonObject }.getOrElse { JsonObject(emptyMap()) }
        val ok = root["ok"]?.jsonPrimitive?.booleanOrNull
        if (!response.isSuccessful || ok == false) {
            val code = root.string("code").ifBlank { "HTTP_${response.code}" }
            val message = root.string("message").ifBlank { "Backend request failed." }
            if (code == "AUTH_REQUIRED" || code == "SESSION_EXPIRED") sessionStore.clear()
            // Do not retain arbitrary server response bodies in exceptions; they can
            // contain account or provider details and may later reach crash/log tooling.
            throw BackendException(code, message, response.code)
        }
        return root
    }

    private suspend fun Call.await(): Response = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        continuation.invokeOnCancellation { cancel() }
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (continuation.isActive) continuation.resume(response) else response.close()
            }
        })
    }

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.int(key: String): Int = this[key]?.jsonPrimitive?.intOrNull ?: 0

    companion object {
        private const val PRODUCT_HEADER = "X-SocialAI-Product"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}

data class ManagedAiResult(
    val reply: String,
    val model: String,
    val dailyUsed: Int,
    val cycleUsed: Int
)
