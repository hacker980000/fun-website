package com.socialaiassistant.keyboard.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


data class EncryptedText(
    val ciphertext: ByteArray,
    val iv: ByteArray
)

interface TextCipher {
    fun encrypt(plaintext: String): EncryptedText
    fun decrypt(value: EncryptedText): String
}

class AesGcmCipher(
    private val keyProvider: () -> SecretKey = { loadOrCreateAndroidKey() }
) : TextCipher {
    override fun encrypt(plaintext: String): EncryptedText {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keyProvider())
        return EncryptedText(
            ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8)),
            iv = cipher.iv.copyOf()
        )
    }

    override fun decrypt(value: EncryptedText): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keyProvider(), GCMParameterSpec(TAG_BITS, value.iv))
        return cipher.doFinal(value.ciphertext).toString(Charsets.UTF_8)
    }

    companion object {
        const val KEY_ALIAS = "social_ai_conversation_key_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128

        private fun loadOrCreateAndroidKey(): SecretKey {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
            generator.init(spec)
            return generator.generateKey()
        }
    }
}
