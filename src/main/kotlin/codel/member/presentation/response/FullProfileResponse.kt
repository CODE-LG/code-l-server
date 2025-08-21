package codel.member.presentation.response

import codel.member.domain.AccessLevel
import codel.member.domain.Member

data class FullProfileResponse(
    val publicProfile: PublicProfileResponse,
    val hiddenProfile: HiddenProfileResponse?,
    val accessLevel: AccessLevel,
    val canViewHidden: Boolean,
    val isMyProfile: Boolean
) {
    companion object {
        fun createPublic(member: Member): FullProfileResponse {
            return FullProfileResponse(
                publicProfile = PublicProfileResponse.from(member),
                hiddenProfile = null,
                accessLevel = AccessLevel.PUBLIC,
                canViewHidden = false,
                isMyProfile = false
            )
        }
        
        fun createFull(member: Member, isMyProfile: Boolean = false): FullProfileResponse {
            val profile = member.getProfileOrThrow()
            return FullProfileResponse(
                publicProfile = PublicProfileResponse.from(member),
                hiddenProfile = if (profile.hiddenCompleted) {
                    HiddenProfileResponse.from(profile)
                } else null,
                accessLevel = if (isMyProfile) AccessLevel.SELF else AccessLevel.CODE_EXCHANGED,
                canViewHidden = true,
                isMyProfile = isMyProfile
            )
        }
    }
}
