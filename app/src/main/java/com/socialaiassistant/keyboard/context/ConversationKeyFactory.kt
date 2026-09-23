package com.socialaiassistant.keyboard.context

import java.security.MessageDigest

data class ConversationKey(val value: String)

class ConversationKeyFactory {
    fun hasStableIdentity(snapshot: ContextSnapshot): Boolean = stableIdentity(snapshot) != null

    fun create(snapshot: ContextSnapshot): ConversationKey {
        val stable = stableIdentity(snapshot)
        val identity = stable ?: ephemeralIdentity(snapshot)
        val material = buildString {
            append(snapshot.packageName.trim().lowercase())
            append('|')
            append(snapshot.surface.name.lowercase())
            append('|')
            append(if (stable != null) "stable:" else "ephemeral:")
            append(identity.lowercase())
        }
        return ConversationKey("conv_${sha256(material)}")
    }

    internal fun identityHash(snapshot: ContextSnapshot): String {
        val stable = stableIdentity(snapshot)
            ?: return sha256("ephemeral|${ephemeralIdentity(snapshot).lowercase()}")
        return sha256("${snapshot.surface.name.lowercase()}|${stable.lowercase()}")
    }

    private fun stableIdentity(snapshot: ContextSnapshot): String? = snapshot.conversationHint
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() && it.length <= 160 }

    private fun ephemeralIdentity(snapshot: ContextSnapshot): String = buildString {
        // This key is intentionally capture-scoped. It is suitable for request fingerprints but
        // ConversationRepository will not persist it as conversation memory.
        append(snapshot.windowSignature.trim().take(500))
        append('|')
        append(snapshot.capturedAtMillis)
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
