package codel.member.presentation.response

import codel.member.domain.Member

data class EssentialProfileResponse(
    val memberId: Long,
    val codeName: String,        // not null
    val age: Int,                // not null
    val sido: String,            // not null
    val sigugun: String,         // not null
    val jobCategory: String,     // not null
    val codeImages: List<String>, // not null
    val interests: List<String>  // not null
) {
    companion object {
        fun from(member: Member): EssentialProfileResponse {
            val profile = member.getProfileOrThrow()
            require(profile.essentialCompleted) { "기본 프로필이 완성되지 않았습니다" }
            
            return EssentialProfileResponse(
                memberId = member.getIdOrThrow(),
                codeName = profile.getCodeNameOrThrow(),
                age = profile.getAge(),
                sido = profile.getBigCityOrThrow(),
                sigugun = profile.getSmallCityOrThrow(),
                jobCategory = profile.getJobOrThrow(),
                codeImages = profile.getCodeImageOrThrow(),
                interests = profile.getInterestsList()
            )
        }
    }
}
