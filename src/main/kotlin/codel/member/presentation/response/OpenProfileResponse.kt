package codel.member.presentation.response

import codel.member.domain.Member

/**
 * 오픈 프로필 (Essential + Personality) - 코드 해제 전 공개되는 정보
 * 얼굴 사진을 제외한 모든 프로필 정보
 */
data class OpenProfileResponse(
    val codeName: String,
    val age: Int,
    val bigCity: String,
    val smallCity: String,
    // Essential Profile
    val job: String,
    val interests: List<String>,
    val codeImages: List<String>,
    val introduce: String?,
    // Personality Profile  
    val hairLength: String,
    val bodyType: String,
    val height: Int,
    val styles: List<String>,
    val mbti: String,
    val drinkingStyle: String,
    val smokingStyle: String,
    val personalities: List<String>,
    val representativeQuestion: String,
    val representativeAnswer: String
) {
    companion object {
        fun from(member: Member): OpenProfileResponse {
            val profile = member.getProfileOrThrow()
            require(profile.isPublicProfileComplete()) { "오픈 프로필이 완성되지 않았습니다" }
            
            return OpenProfileResponse(
                codeName = profile.getCodeNameOrThrow(),
                age = profile.getAge(),
                bigCity = profile.getBigCityOrThrow(),
                smallCity = profile.getSmallCityOrThrow(),
                // Essential
                job = profile.getJobOrThrow(),
                interests = profile.getInterestsList(),
                codeImages = profile.getCodeImageOrThrow(),
                introduce = profile.introduce,
                // Personality
                hairLength = profile.getHairLengthOrThrow(),
                bodyType = profile.getBodyTypeOrThrow(),
                height = profile.getHeightOrThrow(),
                styles = profile.getStylesList(),
                mbti = profile.getMbtiOrThrow(),
                drinkingStyle = profile.getAlcoholOrThrow(),
                smokingStyle = profile.getSmokeOrThrow(),
                personalities = profile.getPersonalitiesList(),
                representativeQuestion = profile.getRepresentativeQuestionOrThrow().content,
                representativeAnswer = profile.getRepresentativeAnswerOrThrow()
            )
        }
    }
}
