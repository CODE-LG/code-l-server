package codel.member.presentation.response

import codel.member.domain.Profile

/**
 * Hidden Profile 정보
 */
data class HiddenProfileResponse(
    val loveLanguage: String,
    val affectionStyle: String,
    val contactStyle: String,
    val dateStyle: String,
    val conflictResolutionStyle: String,
    val relationshipValues: String,
    val faceImages: List<String>
) {
    companion object {
        fun from(profile: Profile): HiddenProfileResponse {
            require(profile.hiddenCompleted) { "Hidden Profile이 완성되지 않았습니다" }
            
            return HiddenProfileResponse(
                loveLanguage = profile.getLoveLanguageOrThrow(),
                affectionStyle = profile.getAffectionStyleOrThrow(),
                contactStyle = profile.getContactStyleOrThrow(),
                dateStyle = profile.getDateStyleOrThrow(),
                conflictResolutionStyle = profile.getConflictResolutionStyleOrThrow(),
                relationshipValues = profile.getRelationshipValuesOrThrow(),
                faceImages = profile.getFaceImageOrThrow()
            )
        }
    }
}
