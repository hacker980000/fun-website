package com.socialaiassistant.keyboard.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretStoreTest {
    @Test
    fun openrouter_key_round_trips_without_plaintext_backing_storage() {
        val backing = InMemorySecretBackingStore()
        val store = SecretStore(backing, ReversibleTestCipher())

        store.putOpenRouterKey("sk-or-test-secret")

        assertEquals("sk-or-test-secret", store.getOpenRouterKey())
        assertFalse(backing.values.values.any { it.contains("sk-or-test-secret") })
        assertEquals("••••cret", store.configuredMask())
        assertTrue(store.isOpenRouterKeyConfigured())
    }

    @Test
    fun blank_key_clears_existing_secret() {
        val backing = InMemorySecretBackingStore()
        val store = SecretStore(backing, ReversibleTestCipher())
        store.putOpenRouterKey("sk-or-test-secret")

        store.putOpenRouterKey("   ")

        assertNull(store.getOpenRouterKey())
        assertFalse(store.isOpenRouterKeyConfigured())
    }

    @Test
    fun clear_removes_ciphertext_iv_and_mask() {
        val backing = InMemorySecretBackingStore()
        val store = SecretStore(backing, ReversibleTestCipher())
        store.putOpenRouterKey("sk-or-12345678")

        store.clearOpenRouterKey()

        assertTrue(backing.values.isEmpty())
        assertNull(store.configuredMask())
    }


    @Test
    fun personal_training_is_encrypted_and_isolated_per_managed_account() {
        val backing = InMemorySecretBackingStore()
        val store = SecretStore(backing, ReversibleTestCipher())

        store.putAccountTraining("account-a", "Short natural Bangla replies")
        store.putAccountTraining("account-b", "Formal English replies")

        assertEquals("Short natural Bangla replies", store.getAccountTraining("account-a"))
        assertEquals("Formal English replies", store.getAccountTraining("account-b"))
        assertFalse(backing.values.keys.any { it.contains("account-a") || it.contains("account-b") })
        assertFalse(backing.values.values.any { it.contains("Short natural Bangla replies") || it.contains("Formal English replies") })

        store.clearAccountTraining("account-a")
        assertNull(store.getAccountTraining("account-a"))
        assertEquals("Formal English replies", store.getAccountTraining("account-b"))
    }

    private class InMemorySecretBackingStore : SecretBackingStore {
        val values = linkedMapOf<String, String>()
        override fun get(key: String): String? = values[key]
        override fun put(key: String, value: String) { values[key] = value }
        override fun remove(key: String) { values.remove(key) }
    }

    private class ReversibleTestCipher : SecretCipher {
        override fun encrypt(plaintext: String): EncryptedText = EncryptedText(
            ciphertext = plaintext.reversed().toByteArray(),
            iv = byteArrayOf(1, 2, 3, 4)
        )

        override fun decrypt(value: EncryptedText): String =
            value.ciphertext.toString(Charsets.UTF_8).reversed()
    }
}
