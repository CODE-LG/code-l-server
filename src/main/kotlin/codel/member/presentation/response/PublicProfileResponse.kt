package codel.member.presentation.response

import codel.member.domain.Member

data class PublicProfileResponse(
    val memberId: Long,
    val codeName: String,        // not null
    val age: Int,                // not null
    val sido: String,            // not null
    val sigugun: String,         // not null
    val jobCategory: String,     // not null
    val codeImages: List<String>, // not null
    val interests: List<String>, // not null
    val hairLength: String,      // not null
    val bodyType: String,        // not null
    val height: Int,             // not null
    val styles: List<String>,    // not null
    val mbti: String,            // not null
    val drinkingStyle: String,   // not null
    val smokingStyle: String,    // not null
    val personalities: List<String>, // not null
    val question: String,        // not null
    val answer: String          // not null
) {
    companion object {
        fun from(member: Member): PublicProfileResponse {
            val profile = member.getProfileOrThrow()
            require(profile.isPublicProfileComplete()) { "공개 프로필이 완성되지 않았습니다" }
            
            return PublicProfileResponse(
                memberId = member.getIdOrThrow(),
                codeName = profile.getCodeNameOrThrow(),
                age = profile.getAge(),
                sido = profile.getBigCityOrThrow(),
                sigugun = profile.getSmallCityOrThrow(),
                jobCategory = profile.getJobOrThrow(),
                codeImages = profile.getCodeImageOrThrow(),
                interests = profile.getInterestsList(),
                hairLength = profile.getHairLengthOrThrow(),
                bodyType = profile.getBodyTypeOrThrow(),
                height = profile.getHeightOrThrow(),
                styles = profile.getStylesList(),
                mbti = profile.getMbtiOrThrow(),
                drinkingStyle = profile.getAlcoholOrThrow(),
                smokingStyle = profile.getSmokeOrThrow(),
                personalities = profile.getPersonalitiesList(),
                question = profile.getQuestionOrThrow(),
                answer = profile.getAnswerOrThrow()
            )
        }
    }
}
