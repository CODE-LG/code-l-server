package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import codel.member.exception.MemberException
import org.springframework.http.HttpStatus.*

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
                    ?: throw MemberException(BAD_REQUEST, "회원 프로필이 존재하지 않습니다.")

            val deserializeHobby = Profile.deserializeAttribute(profile.hobby)
            val deserializeStyle = Profile.deserializeAttribute(profile.style)
            return MemberProfileResponse(
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
                codeImages = profile.getCodeImage() ?: emptyList(),
                faceImages = profile.getFaceImage() ?: emptyList(),
            )
        }
    }
}
