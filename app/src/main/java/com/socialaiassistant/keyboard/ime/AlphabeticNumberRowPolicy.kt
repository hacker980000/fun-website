package com.socialaiassistant.keyboard.ime

/**
 * Single source of truth for the optional 1-0 row above alphabetic keys.
 *
 * The user preference applies only to LETTERS surfaces. Dedicated NUMBER and SYMBOL
 * surfaces never receive an extra number row. Because theme selection happens after
 * layout selection, this policy is shared by every complete Theme Package.
 */
internal object AlphabeticNumberRowPolicy {
    fun shouldShow(mode: KeyboardUiMode, enabledInSettings: Boolean): Boolean {
        if (!enabledInSettings || mode.layer != KeyboardLayer.LETTERS) return false

        return when (mode.language) {
            KeyboardLanguage.ENGLISH -> true
            KeyboardLanguage.BANGLA -> when (mode.banglaMode) {
                BanglaInputMode.PHONETIC,
                BanglaInputMode.BIJOY -> true
            }
        }
    }
}
