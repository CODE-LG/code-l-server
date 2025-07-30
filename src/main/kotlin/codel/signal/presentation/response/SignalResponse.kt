package codel.signal.presentation.response

import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus

data class SignalResponse(
    val id: Long,
    val fromMemberId: Long,
    val toMemberId: Long,
    val senderStatus: SignalStatus,
    val receiverStatus : SignalStatus,
    val toMemberFcmToken: String?,
) {
    companion object {
        fun from(signal: Signal) = SignalResponse(
            id = signal.getIdOrThrow(),
            fromMemberId = signal.fromMember.getIdOrThrow(),
            toMemberId = signal.toMember.getIdOrThrow(),
            senderStatus = signal.senderStatus,
            receiverStatus = signal.receiverStatus,
            toMemberFcmToken = signal.toMember.fcmToken
        )
    }
} 