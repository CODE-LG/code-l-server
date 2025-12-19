package codel.member.presentation.response

import codel.member.domain.AccessLevel
import codel.member.domain.Member
import codel.member.domain.OauthType

/**
 * 완전한 프로필 (오픈 + 숨김)
 */
data class FullProfileResponse(
    val memberId: Long,
    val openProfile: OpenProfileResponse,
    val hiddenProfile: HiddenProfileResponse?,
    val accessLevel: AccessLevel,
    val isMyProfile: Boolean = false,
    val oauthType : OauthType?
) {
    companion object {
        fun createOpen(member: Member): FullProfileResponse {
            return FullProfileResponse(
                memberId = member.getIdOrThrow(),
                openProfile = OpenProfileResponse.from(member),
                hiddenProfile = HiddenProfileResponse.from(member.getProfileOrThrow()),
                accessLevel = AccessLevel.PUBLIC,
                isMyProfile = false,
                oauthType = null,
            )
        }
        
        fun createFull(member: Member, isMyProfile: Boolean = false): FullProfileResponse {
            val profile = member.getProfileOrThrow()
            return FullProfileResponse(
                memberId = member.getIdOrThrow(),
                openProfile = OpenProfileResponse.from(member),
                hiddenProfile = if (profile.hiddenCompleted) {
                    HiddenProfileResponse.from(profile)
                } else null,
                accessLevel = if (isMyProfile) AccessLevel.SELF else AccessLevel.CODE_EXCHANGED,
                isMyProfile = isMyProfile,
                oauthType = if(isMyProfile) member.oauthType else null,
            )
        }
    }
}
