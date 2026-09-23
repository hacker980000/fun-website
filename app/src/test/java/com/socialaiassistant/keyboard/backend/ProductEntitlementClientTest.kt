package com.socialaiassistant.keyboard.backend

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.crypto.EncryptedText
import com.socialaiassistant.keyboard.crypto.SecretBackingStore
import com.socialaiassistant.keyboard.crypto.SecretCipher
import com.socialaiassistant.keyboard.crypto.SecretStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProductEntitlementClientTest {
    private lateinit var server: MockWebServer
    private lateinit var sessionStore: ManagedSessionStore
    private lateinit var installationIdentity: InstallationIdentity

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val context = ApplicationProvider.getApplicationContext<Context>()
        sessionStore = ManagedSessionStore(SecretStore(InMemoryBackingStore(), IdentityCipher()))
        sessionStore.save(
            ManagedSession(
                token = "managed-test-token",
                expiresAt = "2026-10-19T00:00:00Z",
                userId = "user-1",
                email = "user@example.com",
                mobile = "01700000000"
            )
        )
        installationIdentity = InstallationIdentity(context)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun managed_ai_request_identifies_keyboard_product() = runTest {
        server.enqueue(ok("""{"reply":"hello","model":"managed","usage":{"daily":1,"cycle":2}}"""))
        val client = client()

        client.generate(buildJsonObject { put("type", "SMART") })

        val request = server.takeRequest()
        assertEquals("KEYBOARD", request.getHeader("X-SocialAI-Product"))
        assertEquals("/api/v1/ai/generate", request.path)
    }

    @Test
    fun account_state_parses_independent_entitlements_and_identifies_keyboard() = runTest {
        server.enqueue(ok("""
            {
              "user":{"id":"user-1","email":"user@example.com","mobile":"01700000000"},
              "subscription":{"effectiveStatus":"ACTIVE","cycle_expires_at":"2026-10-19T00:00:00Z"},
              "entitlements":[
                {"productCode":"KEYBOARD","status":"ACTIVE","cycle_expires_at":"2026-10-19T00:00:00Z"},
                {"productCode":"ASSISTANT_PRO","status":"EXPIRED","cycle_expires_at":"2026-09-01T00:00:00Z"}
              ],
              "release":{"currentVersion":"35.3.1","minimumVersion":"35.0.0"}
            }
        """.trimIndent()))
        server.enqueue(ok("""{"limits":{"daily":200,"cycle":4000},"daily":{"request_count":3},"cycle":{"request_count":41}}"""))
        val client = client()

        val state = client.accountState()

        assertEquals("ACTIVE", state.keyboardEntitlement.status)
        assertEquals("2026-10-19T00:00:00Z", state.keyboardEntitlement.cycleExpiresAt)
        assertEquals("EXPIRED", state.assistantProEntitlement.status)
        assertEquals("2026-09-01T00:00:00Z", state.assistantProEntitlement.cycleExpiresAt)
        assertEquals("KEYBOARD", server.takeRequest().getHeader("X-SocialAI-Product"))
        assertEquals("KEYBOARD", server.takeRequest().getHeader("X-SocialAI-Product"))
    }

    @Test
    fun legacy_response_without_entitlements_preserves_existing_access_during_rollout() = runTest {
        server.enqueue(ok("""
            {
              "user":{"id":"user-1","email":"user@example.com","mobile":"01700000000"},
              "subscription":{"effectiveStatus":"ACTIVE","cycle_expires_at":"2026-10-19T00:00:00Z"}
            }
        """.trimIndent()))
        server.enqueue(ok("""{"limits":{"daily":200,"cycle":4000},"daily":{"request_count":0},"cycle":{"request_count":0}}"""))

        val state = client().accountState()

        assertEquals("ACTIVE", state.keyboardEntitlement.status)
        assertEquals("ACTIVE", state.assistantProEntitlement.status)
    }

    @Test
    fun explicit_empty_entitlements_does_not_inherit_legacy_subscription() = runTest {
        server.enqueue(ok("""
            {
              "user":{"id":"user-1","email":"user@example.com","mobile":"01700000000"},
              "subscription":{"effectiveStatus":"ACTIVE","cycle_expires_at":"2026-10-19T00:00:00Z"},
              "entitlements":[]
            }
        """.trimIndent()))
        server.enqueue(ok("""{"limits":{"daily":200,"cycle":4000},"daily":{"request_count":0},"cycle":{"request_count":0}}"""))

        val state = client().accountState()

        assertEquals("INACTIVE", state.keyboardEntitlement.status)
        assertEquals("INACTIVE", state.assistantProEntitlement.status)
    }

    @Test
    fun product_access_required_has_clear_keyboard_message() {
        val message = BackendErrorMessages.userMessage(
            BackendException("PRODUCT_ACCESS_REQUIRED", "missing product", 403)
        )
        assertTrue(message.contains("Keyboard", ignoreCase = true))
    }

    private fun client() = SocialAiBackendClient(
        client = OkHttpClient(),
        sessionStore = sessionStore,
        installationIdentity = installationIdentity,
        deviceProof = DeviceProofManager(),
        totalTimeoutMs = 2_000,
        apiBase = server.url("/").toString().removeSuffix("/"),
        requestSigner = { _, _, _, _, _ -> "test-device-signature" }
    )

    private fun ok(body: String) = MockResponse()
        .setResponseCode(200)
        .setHeader("Content-Type", "application/json")
        .setBody("""{"ok":true,${body.trim().removePrefix("{").removeSuffix("}")}}""")

    private class InMemoryBackingStore : SecretBackingStore {
        private val values = mutableMapOf<String, String>()
        override fun get(key: String): String? = values[key]
        override fun put(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private class IdentityCipher : SecretCipher {
        override fun encrypt(plaintext: String): EncryptedText =
            EncryptedText(plaintext.toByteArray(), byteArrayOf(1))

        override fun decrypt(value: EncryptedText): String = value.ciphertext.toString(Charsets.UTF_8)
    }
}
