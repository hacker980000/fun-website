package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.settings.AppSettings

sealed interface ConsentBannerState {
    data object Hidden : ConsentBannerState
    data object Required : ConsentBannerState
}

class ConsentBannerPolicy {
    fun resolve(settings: AppSettings, safety: FieldSafety): ConsentBannerState {
        if (safety == FieldSafety.BLOCK_AI) return ConsentBannerState.Hidden
        val ready = settings.aiPrivacyConsent &&
            settings.contextAccessConsent &&
            settings.accessibilityDisclosureAccepted
        return if (ready) ConsentBannerState.Hidden else ConsentBannerState.Required
    }
}
