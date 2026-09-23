package com.socialaiassistant.keyboard.ime

import java.util.concurrent.atomic.AtomicReference

interface BubbleFlightSink {
    fun submit(request: BubbleFlightRequest): Boolean
    fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean
    fun cancelEditor(editor: BubbleFlightEditorToken)
    fun cancelAll()
}

object BubbleFlightBus {
    private val sink = AtomicReference<BubbleFlightSink?>(null)

    fun register(value: BubbleFlightSink) {
        sink.set(value)
    }

    fun unregister(value: BubbleFlightSink) {
        sink.compareAndSet(value, null)
    }

    fun dispatch(request: BubbleFlightRequest): Boolean = sink.get()?.submit(request) == true

    fun retarget(flightId: Long, target: BubbleFlightTarget): Boolean =
        sink.get()?.retarget(flightId, target) == true

    fun cancelEditor(editor: BubbleFlightEditorToken) {
        sink.get()?.cancelEditor(editor)
    }

    fun cancelAll() {
        sink.get()?.cancelAll()
    }

    internal fun clearForTest() {
        sink.set(null)
    }
}
