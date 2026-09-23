package com.socialaiassistant.keyboard.ime

class BubbleFlightTapCoordinator(
    private val dispatch: (PreparedBubbleFlight) -> Unit
) {
    fun commitThenDispatch(prepared: PreparedBubbleFlight?, commit: () -> Unit) {
        commit()
        if (prepared != null) dispatch(prepared)
    }
}
