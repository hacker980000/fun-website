package com.socialaiassistant.keyboard.ime

/**
 * Reconciles the cursor target captured before the key commit with any newer
 * cursor anchor delivered during the commit before the flight is dispatched.
 */
object BubbleFlightExactTargetRefresh {
    fun choose(
        editor: BubbleFlightEditorToken,
        prepared: BubbleFlightTarget?,
        latest: BubbleFlightTarget?
    ): BubbleFlightTarget? {
        val preparedValid = prepared.takeIf { it.isExactFor(editor) }
        val latestValid = latest.takeIf { it.isExactFor(editor) }
        return when {
            preparedValid == null -> latestValid
            latestValid == null -> preparedValid
            latestValid.capturedAtUptimeMs >= preparedValid.capturedAtUptimeMs -> latestValid
            else -> preparedValid
        }
    }

    private fun BubbleFlightTarget?.isExactFor(editor: BubbleFlightEditorToken): Boolean =
        this != null &&
            this.editor == editor &&
            this.source == BubbleFlightTargetSource.CURSOR_ANCHOR &&
            this.point.isFinite()
}
