package codel.signal.presentation.response

import codel.member.presentation.response.MemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class SignalMemberResponse(
    val signalId: Long,
    val fromMember: MemberProfileResponse,
    val status: SignalStatus,
    val createAt: LocalDateTime
) {
    companion object {
        fun from(signal: Signal): SignalMemberResponse {
            return SignalMemberResponse(
                signalId = signal.id!!,
                fromMember = MemberProfileResponse.toResponse(signal.fromMember),
                status = signal.status,
                createAt = signal.createdAt
            )
        }
    }
}