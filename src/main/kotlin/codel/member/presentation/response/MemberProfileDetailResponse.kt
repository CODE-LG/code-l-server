package codel.member.presentation.response

import codel.member.domain.Member
import codel.signal.domain.SignalStatus

/**
 * 프로필 상세 조회 (시그널 상태 포함)
 */
data class MemberProfileDetailResponse(
    val profile: FullProfileResponse,
    val signalStatus: SignalStatus,
    val isUnlocked: Boolean = false
) {
    companion object {
        fun create(
            member: Member, 
            signalStatus: SignalStatus, 
            isUnlocked: Boolean = false
        ): MemberProfileDetailResponse {
            val profileResponse = if (isUnlocked) {
                FullProfileResponse.createFull(member)
            } else {
                FullProfileResponse.createOpen(member)
            }
            
            return MemberProfileDetailResponse(
                profile = profileResponse,
                signalStatus = signalStatus,
                isUnlocked = isUnlocked
            )
        }

        fun createMyProfileResponse(
            member: Member,
        ): MemberProfileDetailResponse {
            val profileResponse = FullProfileResponse.createFull(member, true)

            return MemberProfileDetailResponse(
                profile = profileResponse,
                signalStatus = SignalStatus.NONE,
                isUnlocked = true
            )
        }
    }
}
