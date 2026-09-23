package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.safety.FieldSafety
import com.socialaiassistant.keyboard.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ConsentBannerPolicyTest {
    private val policy = ConsentBannerPolicy()

    @Test fun missing_ai_privacy_shows_banner() {
        val settings = AppSettings(
            aiPrivacyConsent = false,
            contextAccessConsent = true,
            accessibilityDisclosureAccepted = true
        )
        assertEquals(ConsentBannerState.Required, policy.resolve(settings, FieldSafety.ALLOW_AI))
    }

    @Test fun missing_context_consent_shows_banner() {
        val settings = AppSettings(
            aiPrivacyConsent = true,
            contextAccessConsent = false,
            accessibilityDisclosureAccepted = true
        )
        assertEquals(ConsentBannerState.Required, policy.resolve(settings, FieldSafety.ALLOW_AI))
    }

    @Test fun full_consent_hides_banner() {
        val settings = AppSettings(
            aiPrivacyConsent = true,
            contextAccessConsent = true,
            accessibilityDisclosureAccepted = true
        )
        assertEquals(ConsentBannerState.Hidden, policy.resolve(settings, FieldSafety.ALLOW_AI))
    }

    @Test fun sensitive_field_never_prompts_for_consent() {
        assertEquals(ConsentBannerState.Hidden, policy.resolve(AppSettings(), FieldSafety.BLOCK_AI))
    }
}
