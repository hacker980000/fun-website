package com.socialaiassistant.keyboard.safety

/**
 * Android-free semantic classifier for editor metadata.
 *
 * Apps and frameworks expose security intent through a mix of hint text, field names,
 * private IME options, HTML/autofill-like tokens, labels, and bounded EditorInfo extras.
 * This classifier normalizes those representations without looking at the user's typed text.
 */
object SensitiveMetadataClassifier {
    private val lowerToUpperBoundary = Regex("([a-z0-9])([A-Z])")
    private val acronymBoundary = Regex("([A-Z]+)([A-Z][a-z])")
    private val separators = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val spaces = Regex("\\s+")

    private val sensitiveTokens = setOf(
        "password",
        "passcode",
        "otp",
        "pin",
        "cvv",
        "cvc",
        "2fa",
        "mfa",
        "mpin",
        "পাসওয়ার্ড",
        "পাসওয়ার্ড",
        "পাসকোড",
        "ওটিপি",
        "পিন",
        "সিভিভি",
        "সিভিসি"
    ).map { normalize(it) }.toSet()

    private val sensitivePhrases = listOf(
        "one time password",
        "one time code",
        "otp code",
        "verification code",
        "verify code",
        "security code",
        "authentication code",
        "auth code",
        "login code",
        "sign in code",
        "two factor code",
        "two factor authentication",
        "credit card",
        "debit card",
        "card number",
        "card security code",
        "credit card security code",
        "cc number",
        "cc csc",
        "cc cvv",
        "expiry date",
        "expiration date",
        "credit card expiration",
        "payment password",
        "payment pin",
        "transaction pin",
        "banking pin",
        "bank pin",
        "mobile banking pin",
        "atm pin",
        "upi pin",
        "bkash pin",
        "nagad pin",
        "rocket pin",
        "gift card pin",
        "gift card number",
        "sms otp",
        "email otp",
        "app otp",
        "বর্তমান পাসওয়ার্ড",
        "বর্তমান পাসওয়ার্ড",
        "নতুন পাসওয়ার্ড",
        "নতুন পাসওয়ার্ড",
        "ভেরিফিকেশন কোড",
        "যাচাইকরণ কোড",
        "নিরাপত্তা কোড",
        "সিকিউরিটি কোড",
        "কার্ড নম্বর",
        "কার্ড নাম্বার",
        "পেমেন্ট পিন",
        "লেনদেন পিন",
        "ব্যাংকিং পিন",
        "বিকাশ পিন",
        "নগদ পিন"
    ).map { normalize(it) }

    private val benignNumericTokens = setOf(
        "amount",
        "price",
        "quantity",
        "qty",
        "age",
        "count",
        "score",
        "rating",
        "weight",
        "height",
        "distance",
        "duration",
        "percent",
        "percentage",
        "temperature",
        "year",
        "month",
        "day",
        "zip",
        "postal",
        "amounts",
        "পরিমাণ",
        "দাম",
        "বয়স",
        "বয়স",
        "ওজন",
        "উচ্চতা",
        "শতাংশ"
    ).map { normalize(it) }.toSet()

    private val benignNumericPhrases = listOf(
        "postal code",
        "zip code",
        "order quantity",
        "item quantity",
        "total amount",
        "payment amount",
        "invoice amount",
        "unit price",
        "phone number",
        "mobile number",
        "postal number"
    ).map { normalize(it) }

    fun isSensitive(values: Iterable<String?>): Boolean = values.any { raw ->
        val normalized = normalize(raw.orEmpty())
        if (normalized.isBlank()) return@any false
        val tokens = normalized.split(' ').filter(String::isNotBlank).toSet()
        sensitiveTokens.any(tokens::contains) || sensitivePhrases.any { phrase ->
            containsPhrase(normalized, phrase)
        }
    }

    fun isExplicitlyBenignNumeric(values: Iterable<String?>): Boolean = values.any { raw ->
        val normalized = normalize(raw.orEmpty())
        if (normalized.isBlank()) return@any false
        val tokens = normalized.split(' ').filter(String::isNotBlank).toSet()
        benignNumericTokens.any(tokens::contains) || benignNumericPhrases.any { phrase ->
            containsPhrase(normalized, phrase)
        }
    }

    fun normalize(raw: String): String = raw
        .replace(lowerToUpperBoundary, "$1 $2")
        .replace(acronymBoundary, "$1 $2")
        .replace(separators, " ")
        .lowercase()
        .replace(spaces, " ")
        .trim()

    private fun containsPhrase(normalized: String, canonicalPhrase: String): Boolean {
        return normalized == canonicalPhrase ||
            normalized.startsWith("$canonicalPhrase ") ||
            normalized.endsWith(" $canonicalPhrase") ||
            normalized.contains(" $canonicalPhrase ")
    }
}
