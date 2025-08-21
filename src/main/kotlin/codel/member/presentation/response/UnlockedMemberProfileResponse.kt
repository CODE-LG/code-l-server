package codel.member.presentation.response

import codel.member.domain.Member
import java.time.LocalDateTime

data class UnlockedMemberProfileResponse(
    val member: FullProfileResponse,
    val unlockedTime: LocalDateTime,
) {
    companion object {
        fun toResponse(member: Member, unlockedTime: LocalDateTime): UnlockedMemberProfileResponse {
            return UnlockedMemberProfileResponse(
                FullProfileResponse.createFull(member), // 코드 해제된 경우이므로 Full Profile
                unlockedTime
            )
        }
    }
}
