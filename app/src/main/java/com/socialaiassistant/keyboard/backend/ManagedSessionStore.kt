package com.socialaiassistant.keyboard.backend

import com.socialaiassistant.keyboard.crypto.SecretStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class ManagedSessionStore(private val secretStore: SecretStore) {
    fun save(session: ManagedSession) {
        val value = buildJsonObject {
            put("token", session.token)
            put("expiresAt", session.expiresAt)
            put("userId", session.userId)
            put("email", session.email)
            put("mobile", session.mobile)
        }.toString()
        secretStore.putManagedSession(value)
    }

    fun load(): ManagedSession? {
        val raw = secretStore.getManagedSession()?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching {
            val root = Json.parseToJsonElement(raw).jsonObject
            ManagedSession(
                token = root["token"]?.jsonPrimitive?.content.orEmpty(),
                expiresAt = root["expiresAt"]?.jsonPrimitive?.content.orEmpty(),
                userId = root["userId"]?.jsonPrimitive?.content.orEmpty(),
                email = root["email"]?.jsonPrimitive?.content.orEmpty(),
                mobile = root["mobile"]?.jsonPrimitive?.content.orEmpty()
            )
        }.getOrNull()?.takeIf { it.token.isNotBlank() && it.userId.isNotBlank() }
    }

    fun clear() = secretStore.clearManagedSession()
    fun isLoggedIn(): Boolean = load() != null
}
