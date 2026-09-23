package com.socialaiassistant.keyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeCatalogTest {
    @Test
    fun exposes_exactly_four_packs_and_seven_surfaces() {
        assertEquals(4, ThemePack.entries.size)
        assertEquals(7, KeyboardThemeSurface.entries.size)
    }

    @Test
    fun every_pack_resolves_all_28_unique_surface_variants() {
        val variants = ThemePack.entries.flatMap { pack ->
            KeyboardThemeSurface.entries.map { surface -> ThemeCatalog.surface(pack, surface) }
        }
        assertEquals(28, variants.size)
        assertEquals(28, variants.map { it.id }.toSet().size)
        assertTrue(variants.all { it.isValid() })
    }

    @Test
    fun premium_families_are_not_color_only_clones() {
        val surfaces = ThemePack.entries.map { ThemeCatalog.surface(it, KeyboardThemeSurface.ENGLISH) }
        assertEquals(4, surfaces.map { it.keyCornerRadiusDp to it.fillStyle }.toSet().size)
    }
}
