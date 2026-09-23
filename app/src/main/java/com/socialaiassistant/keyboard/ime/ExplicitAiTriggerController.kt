package com.socialaiassistant.keyboard.ime

import com.socialaiassistant.keyboard.safety.FieldSafety

/**
 * Legacy compatibility policy for the toolbar AI entry point.
 *
 * Opening the AI panel is navigation only. Generation must be started by an
 * explicit Smart/Unique/Flirty/Funny/Rewrite/Translate/Grammar/Caption action.
 */
internal class ExplicitAiTriggerController(
    @Suppress("UNUSED_PARAMETER") requestGeneration: () -> Unit
) {
    fun onToolbarTap(safety: FieldSafety, loading: Boolean): Boolean {
        if (safety == FieldSafety.BLOCK_AI || loading) return false
        return false
    }
}
