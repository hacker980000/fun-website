package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.settings.BubbleKeyIntensity
import org.junit.Assert.assertTrue
import org.junit.Test

class BubbleKeyPolicyTest {
    private val base = BubbleKeyRequest(true, "a", false, false, true, true, BubbleKeyIntensity.NORMAL)

    @Test fun flightMotionScalesByIntensity() {
        val soft = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.SOFT))!!
        val normal = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.NORMAL))!!
        val playful = BubbleKeyPolicy.resolve(base.copy(intensity = BubbleKeyIntensity.PLAYFUL))!!
        assertTrue(soft.flightCurveDp < normal.flightCurveDp)
        assertTrue(normal.flightCurveDp < playful.flightCurveDp)
        assertTrue(soft.flightEndScale > normal.flightEndScale)
        assertTrue(normal.flightEndScale > playful.flightEndScale)
    }
}
