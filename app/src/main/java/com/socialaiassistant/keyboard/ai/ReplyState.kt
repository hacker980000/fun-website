package com.socialaiassistant.keyboard.ai

sealed interface ReplyState {
    data object Hidden : ReplyState
    data object WaitingForContext : ReplyState
    data object Loading : ReplyState
    data class Ready(val reply: String, val fingerprint: String) : ReplyState
    data object Offline : ReplyState
    data object NeedsApiKey : ReplyState
    data class Error(val message: String) : ReplyState
}
