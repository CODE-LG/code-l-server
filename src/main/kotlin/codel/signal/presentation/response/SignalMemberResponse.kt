package codel.signal.presentation.response

import codel.member.domain.Member
import codel.member.presentation.response.MemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class SignalMemberResponse(
    val signalId: Long,
    val member: MemberProfileResponse,
    val status : SignalStatus,
    val createAt: LocalDateTime
) {
    companion object {
        fun fromSend(signal: Signal): SignalMemberResponse =
            SignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = MemberProfileResponse.toResponse(signal.toMember),
                status = signal.senderStatus,
                createAt = signal.createdAt
            )

        fun fromReceive(signal: Signal): SignalMemberResponse =
            SignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = MemberProfileResponse.toResponse(signal.fromMember),
                status = signal.receiverStatus,
                createAt = signal.createdAt
            )
    }
}