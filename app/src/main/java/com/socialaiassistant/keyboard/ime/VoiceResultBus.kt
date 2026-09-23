package com.socialaiassistant.keyboard.ime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface VoiceResult {
    data class Text(val value: String, val target: DeferredInsertionTarget) : VoiceResult
    data class Cancelled(val target: DeferredInsertionTarget?) : VoiceResult
}

object VoiceResultBus {
    private val _pending = MutableStateFlow<VoiceResult?>(null)
    val pending: StateFlow<VoiceResult?> = _pending.asStateFlow()

    fun publish(result: VoiceResult) { _pending.value = result }
    fun consume(): VoiceResult? = _pending.value.also { _pending.value = null }
}
