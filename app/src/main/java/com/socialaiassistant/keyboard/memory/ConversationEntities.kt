package com.socialaiassistant.keyboard.memory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val key: String,
    val packageName: String,
    val identityHash: String,
    val lastUpdatedAtMillis: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["key"],
            childColumns = ["conversationKey"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["conversationKey"]),
        Index(value = ["dedupeHash"], unique = true)
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationKey: String,
    val sender: String,
    val ciphertext: ByteArray,
    val iv: ByteArray,
    val dedupeHash: String,
    val capturedAtMillis: Long,
    val timestampHint: String?
)

data class ConversationHistoryMessage(
    val sender: String,
    val text: String,
    val capturedAtMillis: Long,
    val timestampHint: String?
)

data class ConversationHistory(
    val key: com.socialaiassistant.keyboard.context.ConversationKey,
    val packageName: String,
    val messages: List<ConversationHistoryMessage>
)
