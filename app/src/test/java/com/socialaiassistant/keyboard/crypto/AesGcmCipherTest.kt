package com.socialaiassistant.keyboard.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Test
import javax.crypto.KeyGenerator

class AesGcmCipherTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = AesGcmCipher { key }

    @Test
    fun ciphertext_does_not_contain_plaintext_and_round_trips() {
        val encrypted = cipher.encrypt("secret message")
        assertFalse(encrypted.ciphertext.decodeToString().contains("secret message"))
        assertEquals("secret message", cipher.decrypt(encrypted))
    }

    @Test
    fun every_encryption_uses_a_fresh_iv() {
        val first = cipher.encrypt("same text")
        val second = cipher.encrypt("same text")
        assertNotEquals(first.iv.toList(), second.iv.toList())
    }
}
