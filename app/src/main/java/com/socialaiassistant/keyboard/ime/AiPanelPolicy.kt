package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.ai.TonePreset
import com.socialaiassistant.keyboard.context.ConversationSurface

data class AiSocialLabels(
    val smart: String,
    val witty: String,
    val flirty: String
)

object AiPanelPolicy {
    fun translateTarget(language: KeyboardLanguage): String = when (language) {
        KeyboardLanguage.ENGLISH -> "English"
        KeyboardLanguage.BANGLA -> "Bengali"
    }

    fun socialLabels(surface: ConversationSurface): AiSocialLabels = if (surface == ConversationSurface.COMMENT) {
        AiSocialLabels(
            smart = "Smart Comment",
            witty = "Unique Comment",
            flirty = "Flirty Comment"
        )
    } else {
        AiSocialLabels(
            smart = "Smart Reply",
            witty = "Unique Reply",
            flirty = "Flirty Reply"
        )
    }

    fun socialLabels(isComment: Boolean): AiSocialLabels = socialLabels(
        if (isComment) ConversationSurface.COMMENT else ConversationSurface.INBOX
    )

    fun toneButtonLabel(tonePreset: TonePreset): String = "Tone: ${tonePreset.label}"

    fun nextTone(tonePreset: TonePreset): TonePreset = tonePreset.next()
}
