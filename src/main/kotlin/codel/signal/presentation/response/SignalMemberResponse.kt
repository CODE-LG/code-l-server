package codel.signal.presentation.response

import codel.member.domain.Member
import codel.member.presentation.response.MemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class SignalMemberResponse(
    val signalId: Long,
    val member: MemberProfileResponse,
    val senderStatus: SignalStatus,
    val receiverStatus: SignalStatus,
    val createAt: LocalDateTime
) {
    companion object {
        fun from(signal: Signal, me: Member): SignalMemberResponse {
            val opponent = when (me.id) {
                signal.fromMember.id -> signal.toMember
                signal.toMember.id -> signal.fromMember
                else -> throw IllegalArgumentException("해당 Signal과 관련 없는 사용자입니다.")
            }

            return SignalMemberResponse(
                signalId = signal.getIdOrThrow(),
                member = MemberProfileResponse.toResponse(opponent),
                senderStatus = signal.senderStatus,
                receiverStatus = signal.receiverStatus,
                createAt = signal.createdAt
            )
        }
    }
}