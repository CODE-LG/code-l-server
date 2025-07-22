package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile

data class MemberProfileResponse(
    val memberId : Long,
    val codeName: String,
    val age: Int,
    val job: String,
    val alcohol: String,
    val smoke: String,
    val hobby: List<String>,
    val style: List<String>,
    val bigCity: String,
    val smallCity: String,
    val mbti: String,
    val introduce: String,
    val question: String,
    val answer: String,
    val codeImages: List<String>,
    val faceImages: List<String>,
) {
    companion object {
        fun toResponse(member: Member): MemberProfileResponse {
            val profile = member.getProfileOrThrow()

            val deserializeHobby = Profile.deserializeAttribute(profile.hobby)
            val deserializeStyle = Profile.deserializeAttribute(profile.style)
            return MemberProfileResponse(
                memberId = member.getIdOrThrow(),
                codeName = profile.codeName,
                age = profile.age,
                job = profile.job,
                alcohol = profile.alcohol,
                smoke = profile.smoke,
                hobby = deserializeHobby,
                style = deserializeStyle,
                bigCity = profile.bigCity,
                smallCity = profile.smallCity,
                mbti = profile.mbti,
                introduce = profile.introduce,
                question = profile.question,
                answer = profile.answer,
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }
    }
}
