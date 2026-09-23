package com.socialaiassistant.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlideHitMapTest {
    @Test
    fun immutable_geometry_resolves_expected_key() {
        val map = GlideHitMap(
            listOf(
                GlideHitBox('a', 0f, 0f, 40f, 40f),
                GlideHitBox('s', 41f, 0f, 80f, 40f)
            )
        )
        assertEquals('a', map.keyAt(20f, 20f))
        assertEquals('s', map.keyAt(60f, 20f))
        assertNull(map.keyAt(120f, 20f))
    }
    @Test
    fun small_visual_gap_can_resolve_with_bounded_tolerance() {
        val map = GlideHitMap(
            listOf(
                GlideHitBox('a', 0f, 0f, 40f, 40f),
                GlideHitBox('s', 50f, 0f, 90f, 40f)
            )
        )
        assertNull(map.keyAt(45f, 20f))
        assertEquals('a', map.keyAt(45f, 20f, 6f))
        assertEquals('s', map.keyAt(47f, 20f, 6f))
        assertNull(map.keyAt(45f, 20f, 4f))
    }

}
