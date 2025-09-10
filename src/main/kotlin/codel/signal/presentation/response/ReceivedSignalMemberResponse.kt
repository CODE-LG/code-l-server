package codel.signal.presentation.response

import codel.member.presentation.response.FullProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class ReceivedSignalMemberResponse(
    val signalId: Long,
    val member: FullProfileResponse,
    val signalMessageBySender : String,
    val status: SignalStatus,
    val createAt: String
) {
    companion object {
        fun fromSend(signal: Signal): ReceivedSignalMemberResponse =
            ReceivedSignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = FullProfileResponse.createOpen(signal.toMember), // 시그널 단계에서는 Open만
                signalMessageBySender = signal.message,
                status = signal.senderStatus,
                createAt = signal.createdAt.toString()
            )

        fun fromReceive(signal: Signal): ReceivedSignalMemberResponse =
            ReceivedSignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = FullProfileResponse.createOpen(signal.fromMember), // 시그널 단계에서는 Open만
                signalMessageBySender = signal.message,
                status = signal.receiverStatus,
                createAt = signal.createdAt.toString()
            )
    }
}
