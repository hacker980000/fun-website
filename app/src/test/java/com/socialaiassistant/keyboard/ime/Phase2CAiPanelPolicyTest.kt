package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.ai.TonePreset
import com.socialaiassistant.keyboard.context.ConversationSurface

object Phase2CAiPanelPolicyTest {
    @JvmStatic
    fun main(args: Array<String>) {
        labelsFollowSurface()
        toneButtonCyclesDeterministically()
        println("Phase2CAiPanelPolicyTest PASS")
    }

    private fun labelsFollowSurface() {
        val comment = AiPanelPolicy.socialLabels(ConversationSurface.COMMENT)
        val inbox = AiPanelPolicy.socialLabels(ConversationSurface.INBOX)
        val general = AiPanelPolicy.socialLabels(ConversationSurface.GENERAL)
        check(comment.smart == "Smart Comment")
        check(comment.witty == "Unique Comment")
        check(inbox.smart == "Smart Reply")
        check(general.smart == "Smart Reply")
    }

    private fun toneButtonCyclesDeterministically() {
        check(AiPanelPolicy.toneButtonLabel(TonePreset.AUTO) == "Tone: Auto")
        check(AiPanelPolicy.nextTone(TonePreset.AUTO) == TonePreset.FRIENDLY)
        check(AiPanelPolicy.nextTone(TonePreset.WARM) == TonePreset.AUTO)
    }
}
