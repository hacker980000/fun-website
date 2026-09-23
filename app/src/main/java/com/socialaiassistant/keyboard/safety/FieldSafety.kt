package com.socialaiassistant.keyboard.safety

enum class FieldSafety {
    ALLOW_AI,
    BLOCK_AI,
    NO_CONVERSATION
}

data class EditorDescriptor(
    val inputType: Int,
    val packageName: String?,
    val hintText: String?,
    val accessibilityPassword: Boolean,
    val privateImeOptions: String? = null,
    val autofillHints: List<String> = emptyList(),
    val labelText: String? = null,
    val fieldName: String? = null,
    val actionLabel: String? = null,
    val editorMetadataHints: List<String> = emptyList()
)
