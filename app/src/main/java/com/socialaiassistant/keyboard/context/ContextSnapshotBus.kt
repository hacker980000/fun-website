package com.socialaiassistant.keyboard.context

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ContextSnapshotBus {
    private val _snapshots = MutableStateFlow<ContextSnapshot?>(null)
    val snapshots: StateFlow<ContextSnapshot?> = _snapshots.asStateFlow()

    fun publish(snapshot: ContextSnapshot) {
        _snapshots.value = snapshot
    }

    fun clear() {
        _snapshots.value = null
    }
}

object ContextAccessGate {
    private val _allowed = MutableStateFlow(false)
    val allowed: StateFlow<Boolean> = _allowed.asStateFlow()

    fun update(value: Boolean) {
        _allowed.value = value
        if (!value) ContextSnapshotBus.clear()
    }
}
