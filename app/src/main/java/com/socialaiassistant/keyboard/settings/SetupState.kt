package com.socialaiassistant.keyboard.settings

data class SetupState(
    val keyboardEnabled: Boolean,
    val keyboardSelected: Boolean,
    val accessibilityEnabled: Boolean,
    val contextConsentAccepted: Boolean,
    val personalApiConfigured: Boolean,
    val personalApiMask: String? = null
)
