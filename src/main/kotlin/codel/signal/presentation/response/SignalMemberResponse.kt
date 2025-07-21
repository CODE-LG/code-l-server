package codel.signal.presentation.response

import codel.member.domain.Member
import codel.member.presentation.response.MemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class SignalMemberResponse(
    val signalId: Long,
    val member: MemberProfileResponse,
    val status: SignalStatus,
    val createAt: LocalDateTime
) {
    companion object {
        fun from(signal: Signal, member : Member): SignalMemberResponse {
            return SignalMemberResponse(
                signalId = signal.id!!,
                member = MemberProfileResponse.toResponse(member),
                status = signal.status,
                createAt = signal.createdAt
            )
        }
    }
}