package codel.member.presentation.response

import codel.member.domain.Member

data class MemberProfileResponse(
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
    val codeImages: List<String>,
    val faceImages: List<String>,
) {
    companion object {
        fun toResponse(member: Member): MemberProfileResponse {
            val profile =
                member.profile
                    ?: throw IllegalArgumentException("회원 프로필이 존재하지 않습니다.")

            return MemberProfileResponse(
                codeName = profile.codeName,
                age = profile.age,
                job = profile.job,
                alcohol = profile.alcohol,
                smoke = profile.smoke,
                hobby = profile.hobby,
                style = profile.style,
                bigCity = profile.bigCity,
                smallCity = profile.smallCity,
                mbti = profile.mbti,
                introduce = profile.introduce,
                codeImages = member.codeImage?.urls ?: emptyList(),
                faceImages = member.faceImage?.urls ?: emptyList(),
            )
        }
    }
}
