package com.socialaiassistant.keyboard.ime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingCaptionDraft(
    val value: String,
    val target: DeferredInsertionTarget
)

object CaptionDraftBus {
    private val _pending = MutableStateFlow<PendingCaptionDraft?>(null)
    val pending: StateFlow<PendingCaptionDraft?> = _pending.asStateFlow()

    fun publish(value: String, target: DeferredInsertionTarget) {
        val clean = value.trim()
        _pending.value = clean.takeIf { it.isNotEmpty() }?.let { PendingCaptionDraft(it, target) }
    }

    fun publish(draft: PendingCaptionDraft) {
        _pending.value = draft.value.trim().takeIf { it.isNotEmpty() }?.let { draft.copy(value = it) }
    }

    fun consume(): PendingCaptionDraft? = _pending.value.also { _pending.value = null }
}
