package codel.member.presentation.response

import codel.member.domain.Profile

data class HiddenProfileResponse(
    val loveLanguage: String,            // not null
    val affectionStyle: String,          // not null
    val contactStyle: String,            // not null
    val dateStyle: String,               // not null
    val conflictResolutionStyle: String, // not null
    val relationshipValues: String,      // not null
    val faceImages: List<String>         // not null
) {
    companion object {
        fun from(profile: Profile): HiddenProfileResponse {
            require(profile.hiddenCompleted) { "히든 프로필이 완성되지 않았습니다" }
            
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
