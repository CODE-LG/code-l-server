package codel.signal.presentation.response

import codel.signal.domain.Signal

data class SignalResponse(
    val id: Long,
    val fromMemberId: Long,
    val toMemberId: Long,
    val status: String,
    val toMemberFcmToken: String?,
) {
    companion object {
        fun from(signal: Signal) = SignalResponse(
            id = signal.id!!,
            fromMemberId = signal.fromMember.id!!,
            toMemberId = signal.toMember.id!!,
            status = signal.status.toString(),
            toMemberFcmToken = signal.toMember.fcmToken
        )
    }
} 