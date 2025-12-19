package codel.signal.presentation.response

import codel.member.presentation.response.FullProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class SignalMemberResponse(
    val signalId: Long,
    val member: FullProfileResponse,
    val status: SignalStatus,
    val createAt: String
) {
    companion object {
        fun fromSend(signal: Signal): SignalMemberResponse =
            SignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = FullProfileResponse.createOpen(signal.toMember), // 시그널 단계에서는 Open만
                status = signal.senderStatus,
                createAt = signal.createdAt.toString()
            )

        fun fromReceive(signal: Signal): SignalMemberResponse =
            SignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = FullProfileResponse.createOpen(signal.fromMember), // 시그널 단계에서는 Open만
                status = signal.receiverStatus,
                createAt = signal.createdAt.toString()
            )
    }
}
