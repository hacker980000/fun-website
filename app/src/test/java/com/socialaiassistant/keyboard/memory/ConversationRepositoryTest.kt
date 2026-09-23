package com.socialaiassistant.keyboard.memory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.context.ContextMessage
import com.socialaiassistant.keyboard.context.ContextSnapshot
import com.socialaiassistant.keyboard.context.SenderClass
import com.socialaiassistant.keyboard.crypto.AesGcmCipher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.KeyGenerator

@RunWith(RobolectricTestRunner::class)
class ConversationRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: ConversationRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        repository = ConversationRepository(
            dao = database.conversationDao(),
            cipher = AesGcmCipher { key },
            dispatcher = Dispatchers.Unconfined
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun merging_same_snapshot_twice_deduplicates_rows_and_keeps_ciphertext_only() = runTest {
        val snapshot = snapshot(
            capturedAtMillis = 1_700_000_000_000L,
            messages = listOf(ContextMessage(SenderClass.RECIPIENT, "hello there"))
        )

        repository.mergeSnapshot(snapshot)
        val history = repository.mergeSnapshot(snapshot)

        assertEquals(1, history.messages.size)
        assertEquals("hello there", history.messages.single().text)
        val stored = database.conversationDao().messagesFor(history.key.value)
        assertEquals(1, stored.size)
        assertFalse(stored.single().ciphertext.decodeToString().contains("hello there"))
    }

    @Test
    fun same_visible_snapshot_in_a_later_time_bucket_does_not_duplicate_history() = runTest {
        val first = snapshot(
            capturedAtMillis = 1_700_000_000_000L,
            messages = listOf(ContextMessage(SenderClass.RECIPIENT, "hello there"))
        )
        val later = first.copy(capturedAtMillis = first.capturedAtMillis + 10 * 60 * 1000L)

        repository.mergeSnapshot(first)
        val history = repository.mergeSnapshot(later)

        assertEquals(listOf("hello there"), history.messages.map { it.text })
    }

    @Test
    fun later_snapshot_preserves_existing_history_and_appends_only_new_tail() = runTest {
        val first = snapshot(
            capturedAtMillis = 1_700_000_000_000L,
            messages = listOf(
                ContextMessage(SenderClass.RECIPIENT, "hello"),
                ContextMessage(SenderClass.SELF, "hi")
            )
        )
        val later = snapshot(
            capturedAtMillis = first.capturedAtMillis + 60_000L,
            messages = listOf(
                ContextMessage(SenderClass.SELF, "hi"),
                ContextMessage(SenderClass.RECIPIENT, "how are you?")
            )
        )

        repository.mergeSnapshot(first)
        val history = repository.mergeSnapshot(later)

        assertEquals(
            listOf("hello", "hi", "how are you?"),
            history.messages.map { it.text }
        )
        assertEquals(3, database.conversationDao().countMessages(history.key.value))
    }

    @Test
    fun identical_messages_inside_one_snapshot_are_preserved_as_distinct_rows() = runTest {
        val history = repository.mergeSnapshot(
            snapshot(
                capturedAtMillis = 1_700_000_000_000L,
                messages = listOf(
                    ContextMessage(SenderClass.RECIPIENT, "ok"),
                    ContextMessage(SenderClass.RECIPIENT, "ok")
                )
            )
        )

        assertEquals(listOf("ok", "ok"), history.messages.map { it.text })
        assertEquals(2, database.conversationDao().countMessages(history.key.value))
    }

    @Test
    fun persisted_history_is_bounded_to_recent_200_messages() = runTest {
        var timestamp = 1_700_001_000_000L
        for (index in 0 until 205) {
            repository.mergeSnapshot(
                snapshot(
                    capturedAtMillis = timestamp++,
                    messages = listOf(ContextMessage(SenderClass.RECIPIENT, "message-$index"))
                )
            )
        }

        val probe = snapshot(
            capturedAtMillis = timestamp,
            messages = emptyList()
        )
        val history = repository.historyFor(probe)

        assertEquals(200, database.conversationDao().countMessages(history.key.value))
        assertEquals(200, history.messages.size)
        assertEquals("message-5", history.messages.first().text)
        assertEquals("message-204", history.messages.last().text)
    }

    @Test
    fun clear_all_removes_conversations_and_messages() = runTest {
        val snapshot = snapshot(
            windowSignature = "w2",
            conversationHint = "Nila",
            capturedAtMillis = 1_700_000_100_000L,
            messages = listOf(ContextMessage(SenderClass.RECIPIENT, "keep this private"))
        )

        val history = repository.mergeSnapshot(snapshot)
        assertEquals(1, database.conversationDao().countMessages(history.key.value))

        repository.clearAll()

        assertEquals(0, database.conversationDao().countMessages(history.key.value))
        assertEquals(0, repository.historyFor(snapshot).messages.size)
    }

    @Test
    fun missing_stable_identity_returns_snapshot_only_and_does_not_persist_messages() = runTest {
        val first = snapshot(
            windowSignature = "shared-chat-activity",
            conversationHint = null,
            capturedAtMillis = 1_700_000_000_000L,
            messages = listOf(ContextMessage(SenderClass.RECIPIENT, "thread A visible text"))
        )
        val second = snapshot(
            windowSignature = "shared-chat-activity",
            conversationHint = null,
            capturedAtMillis = first.capturedAtMillis + 1_000L,
            messages = listOf(ContextMessage(SenderClass.RECIPIENT, "thread B visible text"))
        )

        val firstHistory = repository.mergeSnapshot(first)
        val secondHistory = repository.mergeSnapshot(second)

        assertEquals(listOf("thread A visible text"), firstHistory.messages.map { it.text })
        assertEquals(listOf("thread B visible text"), secondHistory.messages.map { it.text })
        assertEquals(0, database.conversationDao().countMessages(firstHistory.key.value))
        assertEquals(0, database.conversationDao().countMessages(secondHistory.key.value))
    }

    private fun snapshot(
        windowSignature: String = "w1",
        conversationHint: String? = "Mitu",
        capturedAtMillis: Long,
        messages: List<ContextMessage>
    ): ContextSnapshot = ContextSnapshot(
        packageName = "com.chat",
        windowSignature = windowSignature,
        conversationHint = conversationHint,
        messages = messages,
        latestRecipientMessage = messages.lastOrNull { it.sender == SenderClass.RECIPIENT }?.text,
        composerHint = "Message",
        confidence = 1f,
        capturedAtMillis = capturedAtMillis
    )
}
