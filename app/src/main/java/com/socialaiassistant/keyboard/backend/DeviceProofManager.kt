package com.socialaiassistant.keyboard.backend

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Signature
import java.security.interfaces.ECPublicKey

class DeviceProofManager {
    fun publicJwkJson(): String {
        val publicKey = ensureKeyPair().public as ECPublicKey
        val x = fixed32(publicKey.w.affineX)
        val y = fixed32(publicKey.w.affineY)
        return "{\"kty\":\"EC\",\"crv\":\"P-256\",\"x\":\"${base64Url(x)}\",\"y\":\"${base64Url(y)}\",\"ext\":true,\"key_ops\":[\"verify\"]}"
    }

    fun signRequest(method: String, path: String, timestamp: Long, requestId: String, bodyText: String): String {
        val bodyHash = sha256Hex(bodyText)
        val canonical = listOf(
            "SAA-DEVICE-PROOF-V1",
            method.uppercase(),
            path,
            timestamp.toString(),
            requestId,
            bodyHash.lowercase()
        ).joinToString("\n")
        val signer = Signature.getInstance("SHA256withECDSA")
        signer.initSign(ensureKeyPair().private)
        signer.update(canonical.toByteArray(Charsets.UTF_8))
        val der = signer.sign()
        return base64Url(derToP1363(der, 32))
    }

    private fun ensureKeyPair(): KeyPair {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val privateKey = store.getKey(KEY_ALIAS, null)
        val publicKey = store.getCertificate(KEY_ALIAS)?.publicKey
        if (privateKey != null && publicKey != null) return KeyPair(publicKey, privateKey as java.security.PrivateKey)

        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
        generator.initialize(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY)
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                .build()
        )
        return generator.generateKeyPair()
    }

    private fun fixed32(value: BigInteger): ByteArray {
        val raw = value.toByteArray()
        return when {
            raw.size == 32 -> raw
            raw.size == 33 && raw[0] == 0.toByte() -> raw.copyOfRange(1, 33)
            raw.size < 32 -> ByteArray(32 - raw.size) + raw
            else -> raw.copyOfRange(raw.size - 32, raw.size)
        }
    }

    private fun derToP1363(der: ByteArray, coordinateSize: Int): ByteArray {
        var index = 0
        fun next(): Int = der[index++].toInt() and 0xff
        require(next() == 0x30) { "Invalid ECDSA signature sequence" }
        val seqLen = readLength(der, index).also { index += it.second }.first
        require(seqLen > 0 && index + seqLen <= der.size) { "Invalid ECDSA sequence length" }
        require(next() == 0x02) { "Invalid ECDSA r value" }
        val rLen = readLength(der, index).also { index += it.second }.first
        val r = der.copyOfRange(index, index + rLen); index += rLen
        require(next() == 0x02) { "Invalid ECDSA s value" }
        val sLen = readLength(der, index).also { index += it.second }.first
        val s = der.copyOfRange(index, index + sLen)
        return normalizeInteger(r, coordinateSize) + normalizeInteger(s, coordinateSize)
    }

    private fun readLength(bytes: ByteArray, start: Int): Pair<Int, Int> {
        val first = bytes[start].toInt() and 0xff
        if (first < 0x80) return first to 1
        val count = first and 0x7f
        require(count in 1..4 && start + count < bytes.size) { "Invalid DER length" }
        var length = 0
        for (i in 1..count) length = (length shl 8) or (bytes[start + i].toInt() and 0xff)
        return length to (1 + count)
    }

    private fun normalizeInteger(value: ByteArray, size: Int): ByteArray {
        var raw = value
        while (raw.size > 1 && raw[0] == 0.toByte()) raw = raw.copyOfRange(1, raw.size)
        require(raw.size <= size) { "ECDSA coordinate too large" }
        return ByteArray(size - raw.size) + raw
    }

    private fun sha256Hex(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun base64Url(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    companion object {
        private const val KEY_ALIAS = "social_ai_device_proof_p256_v1"
    }
}
