package com.socialaiassistant.keyboard.ime

data class DeferredInsertionTarget(
    val packageName: String,
    val inputType: Int,
    val fieldId: Int,
    val hintText: String?,
    val createdAtNanos: Long = System.nanoTime()
)

enum class DeferredInsertionDecision { ACCEPT, WAIT, REJECT }

object DeferredInsertionExtras {
    const val PACKAGE = "deferred_target_package"
    const val INPUT_TYPE = "deferred_target_input_type"
    const val FIELD_ID = "deferred_target_field_id"
    const val HINT = "deferred_target_hint"
    const val CREATED_AT_NANOS = "deferred_target_created_at_nanos"
}

/**
 * A delayed result may wait while our own helper activity is foreground, but it may only
 * insert after the originating editor fingerprint comes back. Results expire locally.
 */
object DeferredInsertionPolicy {
    const val MAX_AGE_NANOS: Long = 10L * 60L * 1_000_000_000L

    fun decision(
        target: DeferredInsertionTarget?,
        currentPackageName: String?,
        currentInputType: Int?,
        currentFieldId: Int?,
        currentHintText: String?,
        blocked: Boolean,
        nowNanos: Long = System.nanoTime()
    ): DeferredInsertionDecision {
        if (target == null || blocked) return DeferredInsertionDecision.REJECT
        val age = nowNanos - target.createdAtNanos
        if (age < 0L || age > MAX_AGE_NANOS) return DeferredInsertionDecision.REJECT
        if (currentPackageName == null || currentInputType == null || currentFieldId == null) {
            return DeferredInsertionDecision.WAIT
        }
        if (currentPackageName != target.packageName) return DeferredInsertionDecision.WAIT

        if (target.fieldId != 0 || currentFieldId != 0) {
            return if (target.fieldId != 0 && target.fieldId == currentFieldId && target.inputType == currentInputType) {
                DeferredInsertionDecision.ACCEPT
            } else {
                DeferredInsertionDecision.REJECT
            }
        }

        if (target.inputType != currentInputType) return DeferredInsertionDecision.REJECT
        val expectedHint = target.hintText.orEmpty().trim()
        val actualHint = currentHintText.orEmpty().trim()
        if (expectedHint.isNotEmpty() && actualHint.isNotEmpty() && expectedHint != actualHint) {
            return DeferredInsertionDecision.REJECT
        }
        return DeferredInsertionDecision.ACCEPT
    }
}
