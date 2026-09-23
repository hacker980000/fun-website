package com.socialaiassistant.keyboard.ime

data class TypingMutation(
    val directCommit: String? = null,
    val composingText: String? = null,
    val deletePrevious: Boolean = false
)
