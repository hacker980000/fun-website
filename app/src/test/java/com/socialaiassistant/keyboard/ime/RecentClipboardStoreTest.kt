package com.socialaiassistant.keyboard.ime

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.socialaiassistant.keyboard.crypto.EncryptedText
import java.nio.charset.StandardCharsets
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RecentClipboardStoreTest {
    private lateinit var context: Context
    private var now = 1_700_000_000_000L
    private val cipher = XorClipboardCipher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        clearPrefs()
    }

    @After
    fun tearDown() {
        clearPrefs()
    }

    @Test
    fun encrypted_store_round_trips_without_plaintext_preferences() {
        val store = newStore()
        store.record("private clipboard value")

        assertEquals(listOf("private clipboard value"), store.recent())
        val prefs = context.getSharedPreferences(RecentClipboardStore.PREFS_V2, Context.MODE_PRIVATE)
        val persisted = prefs.all.values.joinToString("|")
        assertFalse(persisted.contains("private clipboard value"))
        assertTrue(prefs.contains(RecentClipboardStore.CIPHERTEXT_KEY))
        assertTrue(prefs.contains(RecentClipboardStore.IV_KEY))
    }

    @Test
    fun legacy_plaintext_is_purged_instead_of_migrated() {
        context.getSharedPreferences(RecentClipboardStore.LEGACY_PREFS_V1, Context.MODE_PRIVATE)
            .edit()
            .putString("item_0", "legacy secret")
            .commit()

        val store = newStore()

        assertTrue(store.recent().isEmpty())
        assertTrue(
            context.getSharedPreferences(RecentClipboardStore.LEGACY_PREFS_V1, Context.MODE_PRIVATE)
                .all
                .isEmpty()
        )
    }

    @Test
    fun history_expires_after_retention_window() {
        val store = newStore()
        store.record("temporary")
        now += RecentClipboardStore.RETENTION_MILLIS

        assertTrue(store.recent().isEmpty())
        assertTrue(
            context.getSharedPreferences(RecentClipboardStore.PREFS_V2, Context.MODE_PRIVATE)
                .all
                .isEmpty()
        )
    }

    @Test
    fun record_all_is_bounded_deduplicated_and_preserves_current_order() {
        val store = newStore()
        val source = buildList {
            add("first")
            add("second")
            add("first")
            repeat(RecentClipboardStore.MAX_ITEMS + 5) { add("item-$it") }
        }

        store.recordAll(source)
        val recent = store.recent()

        assertEquals(RecentClipboardStore.MAX_ITEMS, recent.size)
        assertEquals("first", recent[0])
        assertEquals("second", recent[1])
        assertEquals(1, recent.count { it == "first" })
    }

    @Test
    fun clear_removes_encrypted_and_legacy_history() {
        val store = newStore()
        store.record("secret")
        context.getSharedPreferences(RecentClipboardStore.LEGACY_PREFS_V1, Context.MODE_PRIVATE)
            .edit()
            .putString("item_0", "old")
            .commit()

        store.clear()

        assertTrue(context.getSharedPreferences(RecentClipboardStore.PREFS_V2, Context.MODE_PRIVATE).all.isEmpty())
        assertTrue(context.getSharedPreferences(RecentClipboardStore.LEGACY_PREFS_V1, Context.MODE_PRIVATE).all.isEmpty())
    }

    private fun newStore(): RecentClipboardStore =
        RecentClipboardStore(context, cipher = cipher, nowMillis = { now })

    private fun clearPrefs() {
        if (::context.isInitialized) {
            context.getSharedPreferences(RecentClipboardStore.PREFS_V2, Context.MODE_PRIVATE).edit().clear().commit()
            context.getSharedPreferences(RecentClipboardStore.LEGACY_PREFS_V1, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}

private class XorClipboardCipher : ClipboardHistoryCipher {
    override fun encrypt(plaintext: String): EncryptedText = EncryptedText(
        ciphertext = plaintext.toByteArray(StandardCharsets.UTF_8).map { (it.toInt() xor MASK).toByte() }.toByteArray(),
        iv = byteArrayOf(1, 2, 3, 4)
    )

    override fun decrypt(value: EncryptedText): String =
        String(value.ciphertext.map { (it.toInt() xor MASK).toByte() }.toByteArray(), StandardCharsets.UTF_8)

    private companion object {
        const val MASK = 0x5A
    }
}
