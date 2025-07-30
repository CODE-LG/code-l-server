package codel.signal.presentation.response

import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus

data class SignalResponse(
    val id: Long,
    val fromMemberId: Long,
    val toMemberId: Long,
    val status: SignalStatus,
    val toMemberFcmToken: String?,
) {
    companion object {
        fun from(signal: Signal) = SignalResponse(
            id = signal.getIdOrThrow(),
            fromMemberId = signal.fromMember.getIdOrThrow(),
            toMemberId = signal.toMember.getIdOrThrow(),
            status = signal.senderStatus,
            toMemberFcmToken = signal.toMember.fcmToken
        )

        fun fromSend(signal: Signal): SignalResponse {
            return SignalResponse(
                id = signal.getIdOrThrow(),
                fromMemberId = signal.fromMember.getIdOrThrow(),
                toMemberId = signal.toMember.getIdOrThrow(),
                status = signal.senderStatus,
                toMemberFcmToken = signal.toMember.fcmToken  // 푸시는 받는 쪽에게 가야 하므로 유지
            )
        }
    }
} 