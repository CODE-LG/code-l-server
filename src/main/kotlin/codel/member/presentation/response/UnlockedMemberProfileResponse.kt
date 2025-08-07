package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class UnlockedMemberProfileResponse(
    val member : MemberProfileResponse,
    val unlockedTime : LocalDateTime,
) {
    companion object {
        fun toResponse(member: Member, unlockedTime : LocalDateTime): UnlockedMemberProfileResponse {
            return UnlockedMemberProfileResponse(MemberProfileResponse.toResponse(member), unlockedTime)
        }
    }
}
