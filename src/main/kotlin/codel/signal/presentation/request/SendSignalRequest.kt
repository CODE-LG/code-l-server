package codel.signal.presentation.request

data class SendSignalRequest(
    val toMemberId: Long,
    val message : String
)