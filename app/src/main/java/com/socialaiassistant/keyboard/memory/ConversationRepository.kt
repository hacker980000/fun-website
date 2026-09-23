package com.socialaiassistant.keyboard.memory

import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.ConversationKeyFactory
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.crypto.EncryptedText
import com.socialaiassistant.keyboard.crypto.TextCipher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class ConversationRepository(
    private val dao: ConversationDao,
    private val cipher: TextCipher,
    private val keyFactory: ConversationKeyFactory = ConversationKeyFactory(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun mergeSnapshot(snapshot: ContextSnapshot): ConversationHistory = withContext(dispatcher) {
        val key = keyFactory.create(snapshot)

        // Never persist a conversation under windowId/className. Android chat apps commonly
        // reuse the same Activity/window for different threads, so a missing stable title must
        // stay snapshot-local to prevent cross-thread memory contamination.
        if (!keyFactory.hasStableIdentity(snapshot)) {
            return@withContext transientHistory(snapshot, key)
        }

        // @Upsert is intentionally used by the DAO. REPLACE would delete the existing
        // conversation row and cascade-delete all MessageEntity children.
        dao.upsertConversation(
            ConversationEntity(
                key = key.value,
                packageName = snapshot.packageName,
                identityHash = keyFactory.identityHash(snapshot),
                lastUpdatedAtMillis = snapshot.capturedAtMillis
            )
        )

        val existingEntities = recentEntities(key.value)
        val existingFingerprints = existingEntities.map { entity ->
            MessageFingerprint(
                sender = senderClass(entity.sender),
                text = normalizeMessage(cipher.decrypt(EncryptedText(entity.ciphertext, entity.iv))),
                timestampHint = normalizeTimestampHint(entity.timestampHint)
            )
        }
        val incomingMessages = snapshot.messages.mapNotNull { message ->
            val normalized = normalizeMessage(message.text)
            if (normalized.isEmpty()) {
                null
            } else {
                NormalizedMessage(
                    sender = message.sender,
                    text = normalized,
                    timestampHint = normalizeTimestampHint(message.timestampHint)
                )
            }
        }

        // Accessibility snapshots usually contain a rolling window of messages already seen.
        // Preserve history and append only the portion that extends past the stored tail.
        val appendFromIndex = appendStartIndex(
            existing = existingFingerprints,
            incoming = incomingMessages.map { it.fingerprint() }
        )

        incomingMessages.drop(appendFromIndex).forEachIndexed { relativeIndex, message ->
            val snapshotIndex = appendFromIndex + relativeIndex
            val encrypted = cipher.encrypt(message.text)
            val dedupeHash = sha256(
                listOf(
                    key.value,
                    message.sender.name,
                    message.text,
                    message.timestampHint.orEmpty(),
                    snapshot.capturedAtMillis.toString(),
                    snapshotIndex.toString()
                ).joinToString("|")
            )
            dao.insertMessage(
                MessageEntity(
                    conversationKey = key.value,
                    sender = message.sender.name,
                    ciphertext = encrypted.ciphertext,
                    iv = encrypted.iv,
                    dedupeHash = dedupeHash,
                    capturedAtMillis = snapshot.capturedAtMillis,
                    timestampHint = message.timestampHint
                )
            )
        }

        dao.pruneMessages(key.value, MAX_PERSISTED_MESSAGES_PER_CONVERSATION)

        val messages = recentEntities(key.value).map { entity ->
            ConversationHistoryMessage(
                sender = entity.sender,
                text = cipher.decrypt(EncryptedText(entity.ciphertext, entity.iv)),
                capturedAtMillis = entity.capturedAtMillis,
                timestampHint = entity.timestampHint
            )
        }
        ConversationHistory(key, snapshot.packageName, messages)
    }

    suspend fun historyFor(snapshot: ContextSnapshot): ConversationHistory = withContext(dispatcher) {
        val key = keyFactory.create(snapshot)
        if (!keyFactory.hasStableIdentity(snapshot)) {
            return@withContext transientHistory(snapshot, key)
        }
        val messages = recentEntities(key.value).map { entity ->
            ConversationHistoryMessage(
                sender = entity.sender,
                text = cipher.decrypt(EncryptedText(entity.ciphertext, entity.iv)),
                capturedAtMillis = entity.capturedAtMillis,
                timestampHint = entity.timestampHint
            )
        }
        ConversationHistory(key, snapshot.packageName, messages)
    }

    suspend fun clearConversation(snapshot: ContextSnapshot) = withContext(dispatcher) {
        if (keyFactory.hasStableIdentity(snapshot)) {
            dao.deleteConversation(keyFactory.create(snapshot).value)
        }
    }

    suspend fun clearAll() = withContext(dispatcher) {
        dao.clearAllConversations()
    }


    private suspend fun recentEntities(conversationKey: String): List<MessageEntity> =
        dao.recentMessagesFor(conversationKey, MAX_PERSISTED_MESSAGES_PER_CONVERSATION).asReversed()

    private fun transientHistory(snapshot: ContextSnapshot, key: com.socialaiassistant.keyboard.context.ConversationKey): ConversationHistory {
        val messages = snapshot.messages.mapNotNull { message ->
            val normalized = normalizeMessage(message.text)
            if (normalized.isEmpty()) {
                null
            } else {
                ConversationHistoryMessage(
                    sender = message.sender.name,
                    text = normalized,
                    capturedAtMillis = snapshot.capturedAtMillis,
                    timestampHint = normalizeTimestampHint(message.timestampHint)
                )
            }
        }
        return ConversationHistory(key, snapshot.packageName, messages)
    }

    private fun appendStartIndex(
        existing: List<MessageFingerprint>,
        incoming: List<MessageFingerprint>
    ): Int {
        if (existing.isEmpty() || incoming.isEmpty()) return 0

        val maxOverlap = minOf(existing.size, incoming.size)
        for (overlapSize in maxOverlap downTo 1) {
            val existingStart = existing.size - overlapSize
            var matches = true
            for (offset in 0 until overlapSize) {
                if (existing[existingStart + offset] != incoming[offset]) {
                    matches = false
                    break
                }
            }
            if (matches) return overlapSize
        }
        return 0
    }

    private fun normalizeMessage(value: String): String = value
        .replace("\u0000", "")
        .replace(Regex("[ \\t]+\\n"), "\n")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
        .take(MAX_STORED_MESSAGE_CHARS)

    private fun normalizeTimestampHint(value: String?): String? = value
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    private fun senderClass(value: String): SenderClass = runCatching {
        SenderClass.valueOf(value)
    }.getOrDefault(SenderClass.UNKNOWN)

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private data class NormalizedMessage(
        val sender: SenderClass,
        val text: String,
        val timestampHint: String?
    ) {
        fun fingerprint() = MessageFingerprint(sender, text, timestampHint)
    }

    private data class MessageFingerprint(
        val sender: SenderClass,
        val text: String,
        val timestampHint: String?
    )

    private companion object {
        // Prompt construction currently uses a much smaller tail (10-30 messages). Keeping
        // a bounded encrypted buffer preserves useful continuity while preventing years of
        // accessibility snapshots from turning history lookup/decrypt into an unbounded cost.
        const val MAX_PERSISTED_MESSAGES_PER_CONVERSATION = 200
        const val MAX_STORED_MESSAGE_CHARS = 5_000
    }
}
