package com.socialaiassistant.keyboard.ime

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import com.socialaiassistant.keyboard.safety.EditorDescriptor

/**
 * Converts EditorInfo into bounded safety metadata without reading surrounding/typed text.
 *
 * Android's standard EditorInfo exposes hint/label/fieldName/privateImeOptions/extras, but not
 * View.autofillHints directly. Some frameworks mirror semantic/autofill tokens into those fields;
 * relevant bounded extras are therefore included as supplemental hints when present.
 */
internal object EditorInfoSafetyAdapter {
    private const val MAX_HINTS = 24
    private const val MAX_HINT_CHARS = 160

    fun descriptor(editor: EditorInfo): EditorDescriptor = EditorDescriptor(
        inputType = editor.inputType,
        packageName = editor.packageName,
        hintText = editor.hintText?.toString()?.bounded(),
        accessibilityPassword = false,
        privateImeOptions = editor.privateImeOptions?.bounded(),
        labelText = editor.label?.toString()?.bounded(),
        fieldName = editor.fieldName?.bounded(),
        actionLabel = editor.actionLabel?.toString()?.bounded(),
        editorMetadataHints = collectRelevantExtras(editor.extras)
    )

    @Suppress("DEPRECATION")
    private fun collectRelevantExtras(extras: Bundle?): List<String> {
        if (extras == null || extras.isEmpty) return emptyList()
        val out = LinkedHashSet<String>()
        val keys = runCatching { extras.keySet().toList().sorted() }.getOrDefault(emptyList())
        for (key in keys) {
            if (out.size >= MAX_HINTS) break
            val boundedKey = key.bounded()
            if (boundedKey.isBlank()) continue

            // Keys themselves often carry semantic names (otpHint, autofillHints, cardField, etc.).
            out += boundedKey
            if (!looksSafetyRelevantKey(boundedKey)) continue

            val value = runCatching { extras.get(key) }.getOrNull()
            when (value) {
                is CharSequence -> addBounded(out, value.toString())
                is Array<*> -> value.asSequence()
                    .filterIsInstance<CharSequence>()
                    .take(4)
                    .forEach { addBounded(out, it.toString()) }
                is Collection<*> -> value.asSequence()
                    .filterIsInstance<CharSequence>()
                    .take(4)
                    .forEach { addBounded(out, it.toString()) }
            }
        }
        return out.take(MAX_HINTS)
    }

    private fun looksSafetyRelevantKey(key: String): Boolean {
        val normalized = key.lowercase()
        return RELEVANT_KEY_TOKENS.any(normalized::contains)
    }

    private fun addBounded(target: MutableSet<String>, raw: String) {
        if (target.size >= MAX_HINTS) return
        raw.bounded().takeIf(String::isNotBlank)?.let(target::add)
    }

    private fun String.bounded(): String = trim().take(MAX_HINT_CHARS)

    private val RELEVANT_KEY_TOKENS = listOf(
        "autofill",
        "hint",
        "field",
        "input",
        "password",
        "passcode",
        "credential",
        "otp",
        "pin",
        "verification",
        "security",
        "auth",
        "card",
        "cvv",
        "cvc",
        "payment",
        "bank"
    )
}
