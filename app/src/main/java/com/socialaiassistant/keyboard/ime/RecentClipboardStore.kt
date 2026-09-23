package com.socialaiassistant.keyboard.ime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.socialaiassistant.keyboard.crypto.EncryptedText
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONArray
import org.json.JSONObject

internal interface ClipboardHistoryCipher {
    fun encrypt(plaintext: String): EncryptedText
    fun decrypt(value: EncryptedText): String
}

internal class RecentClipboardStore(
    context: Context,
    private val cipher: ClipboardHistoryCipher = AndroidKeystoreClipboardHistoryCipher(),
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_V2, Context.MODE_PRIVATE)
    private val legacyPreferences = appContext.getSharedPreferences(LEGACY_PREFS_V1, Context.MODE_PRIVATE)

    init {
        // Stage 25.2 intentionally does not migrate old plaintext clipboard history.
        // Purge it instead so upgrading users do not retain sensitive plaintext on disk.
        legacyPreferences.edit().clear().apply()
    }

    fun recent(): List<String> = loadFreshRecords().map(ClipboardRecord::text)

    fun record(value: String) = recordAll(listOf(value))

    fun recordAll(values: List<String>) {
        val cleanValues = values
            .map { it.trim().take(MAX_CHARS) }
            .filter(String::isNotEmpty)
            .distinct()
        if (cleanValues.isEmpty()) return

        val capturedAt = nowMillis()
        val incoming = cleanValues.map { ClipboardRecord(text = it, capturedAtMillis = capturedAt) }
        val merged = (incoming + loadFreshRecords())
            .distinctBy(ClipboardRecord::text)
            .take(MAX_ITEMS)
        writeRecords(merged)
    }

    fun clear() {
        preferences.edit().clear().apply()
        legacyPreferences.edit().clear().apply()
    }

    private fun loadFreshRecords(): List<ClipboardRecord> {
        val ciphertext = preferences.getString(CIPHERTEXT_KEY, null)?.decodeBase64OrNull()
        val iv = preferences.getString(IV_KEY, null)?.decodeBase64OrNull()
        if (ciphertext == null || iv == null) {
            if (ciphertext != null || iv != null) preferences.edit().clear().apply()
            return emptyList()
        }

        val plaintext = runCatching {
            cipher.decrypt(EncryptedText(ciphertext = ciphertext, iv = iv))
        }.getOrNull()
        if (plaintext == null) {
            // Corrupt/undecryptable history must fail closed rather than remain on disk.
            preferences.edit().clear().apply()
            return emptyList()
        }

        val decoded = decodePayload(plaintext)
        if (decoded == null) {
            preferences.edit().clear().apply()
            return emptyList()
        }
        val now = nowMillis()
        val fresh = decoded
            .filter { record ->
                val age = now - record.capturedAtMillis
                record.capturedAtMillis > 0L &&
                    age >= -MAX_FUTURE_CLOCK_SKEW_MILLIS &&
                    age < RETENTION_MILLIS
            }
            .distinctBy(ClipboardRecord::text)
            .take(MAX_ITEMS)

        if (fresh.size != decoded.size) writeRecords(fresh)
        return fresh
    }

    private fun writeRecords(records: List<ClipboardRecord>) {
        val bounded = records
            .mapNotNull { record ->
                val clean = record.text.trim().take(MAX_CHARS)
                clean.takeIf(String::isNotEmpty)?.let { ClipboardRecord(it, record.capturedAtMillis) }
            }
            .distinctBy(ClipboardRecord::text)
            .take(MAX_ITEMS)

        if (bounded.isEmpty()) {
            preferences.edit().clear().apply()
            return
        }

        val encrypted = runCatching { cipher.encrypt(encodePayload(bounded)) }.getOrNull()
        if (encrypted == null) {
            // Never fall back to plaintext storage if Android Keystore is unavailable.
            preferences.edit().clear().apply()
            return
        }

        preferences.edit()
            .clear()
            .putString(CIPHERTEXT_KEY, Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP))
            .putString(IV_KEY, Base64.encodeToString(encrypted.iv, Base64.NO_WRAP))
            .apply()
    }

    private fun encodePayload(records: List<ClipboardRecord>): String {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject()
                    .put(JSON_TEXT, record.text)
                    .put(JSON_CAPTURED_AT, record.capturedAtMillis)
            )
        }
        return array.toString()
    }

    private fun decodePayload(payload: String): List<ClipboardRecord>? = runCatching {
        val array = JSONArray(payload)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val text = item.optString(JSON_TEXT, "").trim().take(MAX_CHARS)
                val capturedAt = item.optLong(JSON_CAPTURED_AT, -1L)
                if (text.isNotEmpty() && capturedAt > 0L) {
                    add(ClipboardRecord(text = text, capturedAtMillis = capturedAt))
                }
            }
        }
    }.getOrNull()

    private fun String.decodeBase64OrNull(): ByteArray? =
        runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

    private data class ClipboardRecord(
        val text: String,
        val capturedAtMillis: Long
    )

    companion object {
        const val MAX_ITEMS = 20
        const val MAX_CHARS = 4_000
        const val RETENTION_MILLIS = 24L * 60L * 60L * 1_000L

        internal const val LEGACY_PREFS_V1 = "recent_clipboard_history_v1"
        internal const val PREFS_V2 = "recent_clipboard_history_v2_encrypted"
        internal const val CIPHERTEXT_KEY = "history_ciphertext"
        internal const val IV_KEY = "history_iv"

        private const val JSON_TEXT = "text"
        private const val JSON_CAPTURED_AT = "captured_at"
        private const val MAX_FUTURE_CLOCK_SKEW_MILLIS = 5L * 60L * 1_000L
    }
}

private class AndroidKeystoreClipboardHistoryCipher : ClipboardHistoryCipher {
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
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val specification = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
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
        const val KEY_ALIAS = "social_ai_clipboard_history_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
    }
}
