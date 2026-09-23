package com.socialaiassistant.keyboard.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ConversationDao {
    /**
     * Update the conversation metadata without REPLACE semantics.
     *
     * INSERT ... REPLACE deletes the existing parent row first, which would trigger
     * MessageEntity's ON DELETE CASCADE foreign key and wipe the conversation history.
     * Room @Upsert preserves the parent row identity while updating its metadata.
     */
    @Upsert
    suspend fun upsertConversation(entity: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMessage(entity: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE conversationKey = :conversationKey ORDER BY capturedAtMillis ASC, id ASC")
    suspend fun messagesFor(conversationKey: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE conversationKey = :conversationKey ORDER BY capturedAtMillis DESC, id DESC LIMIT :limit")
    suspend fun recentMessagesFor(conversationKey: String, limit: Int): List<MessageEntity>

    @Query(
        """
        DELETE FROM messages
        WHERE conversationKey = :conversationKey
          AND id NOT IN (
              SELECT id FROM messages
              WHERE conversationKey = :conversationKey
              ORDER BY capturedAtMillis DESC, id DESC
              LIMIT :keep
          )
        """
    )
    suspend fun pruneMessages(conversationKey: String, keep: Int)

    @Query("SELECT COUNT(*) FROM messages WHERE conversationKey = :conversationKey")
    suspend fun countMessages(conversationKey: String): Int

    @Query("DELETE FROM conversations WHERE key = :conversationKey")
    suspend fun deleteConversation(conversationKey: String)

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}
