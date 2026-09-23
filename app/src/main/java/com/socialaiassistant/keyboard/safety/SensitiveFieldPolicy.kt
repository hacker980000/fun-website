package com.socialaiassistant.keyboard.safety

/**
 * Privacy-first editor policy.
 *
 * The policy intentionally does not inspect typed/surrounding text. It relies on editor metadata,
 * input-type semantics, and (when available) Accessibility password metadata supplied in the
 * [EditorDescriptor]. Ambiguous unlabeled numeric editors fail closed because OTP/PIN/CVV fields
 * are frequently implemented as plain numeric inputs.
 */
class SensitiveFieldPolicy {
    fun evaluate(descriptor: EditorDescriptor): FieldSafety {
        if (descriptor.accessibilityPassword) return FieldSafety.BLOCK_AI
        if (isPasswordInputType(descriptor.inputType)) return FieldSafety.BLOCK_AI

        val metadata = descriptor.metadataValues()
        if (SensitiveMetadataClassifier.isSensitive(metadata)) return FieldSafety.BLOCK_AI

        return when (descriptor.inputType and TYPE_MASK_CLASS) {
            TYPE_CLASS_TEXT -> FieldSafety.ALLOW_AI
            TYPE_CLASS_NUMBER -> {
                if (SensitiveMetadataClassifier.isExplicitlyBenignNumeric(metadata)) {
                    FieldSafety.NO_CONVERSATION
                } else {
                    // Fail closed: a plain numeric editor with weak/no semantics can be OTP/PIN/CVV.
                    FieldSafety.BLOCK_AI
                }
            }
            TYPE_CLASS_PHONE, TYPE_CLASS_DATETIME -> FieldSafety.NO_CONVERSATION
            else -> FieldSafety.NO_CONVERSATION
        }
    }

    private fun EditorDescriptor.metadataValues(): List<String?> = buildList {
        add(hintText)
        add(privateImeOptions)
        add(labelText)
        add(fieldName)
        add(actionLabel)
        addAll(autofillHints)
        addAll(editorMetadataHints)
    }

    private fun isPasswordInputType(inputType: Int): Boolean {
        val inputClass = inputType and TYPE_MASK_CLASS
        val variation = inputType and TYPE_MASK_VARIATION

        if (inputClass == TYPE_CLASS_NUMBER && variation == TYPE_NUMBER_VARIATION_PASSWORD) {
            return true
        }

        if (inputClass != TYPE_CLASS_TEXT) return false

        return variation == TYPE_TEXT_VARIATION_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    private companion object {
        const val TYPE_MASK_CLASS = 0x0000000f
        const val TYPE_CLASS_TEXT = 0x00000001
        const val TYPE_CLASS_NUMBER = 0x00000002
        const val TYPE_CLASS_PHONE = 0x00000003
        const val TYPE_CLASS_DATETIME = 0x00000004

        const val TYPE_MASK_VARIATION = 0x00000ff0
        const val TYPE_NUMBER_VARIATION_PASSWORD = 0x00000010
        const val TYPE_TEXT_VARIATION_PASSWORD = 0x00000080
        const val TYPE_TEXT_VARIATION_VISIBLE_PASSWORD = 0x00000090
        const val TYPE_TEXT_VARIATION_WEB_PASSWORD = 0x000000e0
    }
}
