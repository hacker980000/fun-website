package com.socialaiassistant.keyboard.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface SecretBackingStore {
    fun get(key: String): String?
    fun put(key: String, value: String)
    fun remove(key: String)
}

interface SecretCipher {
    fun encrypt(plaintext: String): EncryptedText
    fun decrypt(value: EncryptedText): String
}

class SecretStore(
    private val backingStore: SecretBackingStore,
    private val cipher: SecretCipher
) {
    fun putOpenRouterKey(rawValue: String) {
        val value = rawValue.trim()
        if (value.isEmpty()) {
            clearOpenRouterKey()
            return
        }
        val encrypted = cipher.encrypt(value)
        backingStore.put(CIPHERTEXT_KEY, Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
        backingStore.put(IV_KEY, Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
        backingStore.put(MASK_KEY, maskFor(value))
    }

    fun getOpenRouterKey(): String? {
        val ciphertext = backingStore.get(CIPHERTEXT_KEY)?.let(::decodeBase64) ?: return null
        val iv = backingStore.get(IV_KEY)?.let(::decodeBase64) ?: return null
        return runCatching { cipher.decrypt(EncryptedText(ciphertext, iv)) }
            .getOrNull()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
    }

    fun clearOpenRouterKey() {
        backingStore.remove(CIPHERTEXT_KEY)
        backingStore.remove(IV_KEY)
        backingStore.remove(MASK_KEY)
    }

    fun isOpenRouterKeyConfigured(): Boolean =
        backingStore.get(CIPHERTEXT_KEY) != null && backingStore.get(IV_KEY) != null

    fun configuredMask(): String? = backingStore.get(MASK_KEY)

    fun putManagedSession(rawValue: String) = putEncrypted(MANAGED_SESSION_PREFIX, rawValue)

    fun getManagedSession(): String? = getEncrypted(MANAGED_SESSION_PREFIX)

    fun clearManagedSession() = clearEncrypted(MANAGED_SESSION_PREFIX)

    fun putAccountTraining(accountId: String, value: String) = putEncrypted(accountTrainingPrefix(accountId), value.take(1800))

    fun getAccountTraining(accountId: String): String? = getEncrypted(accountTrainingPrefix(accountId))

    fun clearAccountTraining(accountId: String) = clearEncrypted(accountTrainingPrefix(accountId))

    private fun accountTrainingPrefix(accountId: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(accountId.trim().toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
            .take(24)
        return "personal_training_$digest"
    }

    private fun putEncrypted(prefix: String, rawValue: String) {
        val value = rawValue.trim()
        if (value.isEmpty()) {
            clearEncrypted(prefix)
            return
        }
        val encrypted = cipher.encrypt(value)
        backingStore.put("${prefix}_ciphertext", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
        backingStore.put("${prefix}_iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
    }

    private fun getEncrypted(prefix: String): String? {
        val ciphertext = backingStore.get("${prefix}_ciphertext")?.let(::decodeBase64) ?: return null
        val iv = backingStore.get("${prefix}_iv")?.let(::decodeBase64) ?: return null
        return runCatching { cipher.decrypt(EncryptedText(ciphertext, iv)) }
            .getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun clearEncrypted(prefix: String) {
        backingStore.remove("${prefix}_ciphertext")
        backingStore.remove("${prefix}_iv")
    }

    private fun decodeBase64(value: String): ByteArray? =
        runCatching { Base64.decode(value, Base64.NO_WRAP) }.getOrNull()

    private fun maskFor(value: String): String {
        val suffix = value.takeLast(4)
        return "••••$suffix"
    }

    companion object {
        const val KEY_ALIAS = "social_ai_secret_key_v1"
        private const val PREFS_NAME = "social_ai_secrets"
        private const val CIPHERTEXT_KEY = "openrouter_ciphertext"
        private const val IV_KEY = "openrouter_iv"
        private const val MASK_KEY = "openrouter_mask"
        private const val MANAGED_SESSION_PREFIX = "managed_session_v1"

        fun create(context: Context): SecretStore = SecretStore(
            SharedPreferencesSecretBackingStore(context.applicationContext, PREFS_NAME),
            AndroidKeystoreSecretCipher(KEY_ALIAS)
        )
    }
}

private class SharedPreferencesSecretBackingStore(
    context: Context,
    preferencesName: String
) : SecretBackingStore {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    override fun get(key: String): String? = preferences.getString(key, null)

    override fun put(key: String, value: String) {
        preferences.edit().putString(key, value).apply()
    }

    override fun remove(key: String) {
        preferences.edit().remove(key).apply()
    }
}

private class AndroidKeystoreSecretCipher(
    private val alias: String
) : SecretCipher {
    override fun encrypt(plaintext: String): EncryptedText {
        val engine = Cipher.getInstance(TRANSFORMATION)
        engine.init(Cipher.ENCRYPT_MODE, loadOrCreateKey())
        return EncryptedText(
            ciphertext = engine.doFinal(plaintext.toByteArray(Charsets.UTF_8)),
            iv = engine.iv.copyOf()
        )
    }

    override fun decrypt(value: EncryptedText): String {
        val engine = Cipher.getInstance(TRANSFORMATION)
        engine.init(Cipher.DECRYPT_MODE, loadOrCreateKey(), GCMParameterSpec(TAG_BITS, value.iv))
        return engine.doFinal(value.ciphertext).toString(Charsets.UTF_8)
    }

    private fun loadOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val specification = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(specification)
        return generator.generateKey()
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
