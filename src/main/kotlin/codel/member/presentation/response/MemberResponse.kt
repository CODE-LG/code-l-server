package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import java.time.LocalDateTime

data class MemberResponse(
    val memberId: Long,
    val codeImage: String,
    val faceImage: String,
    val name: String,
    val age: Int,
    val introduce: String,
    val bigCity: String,
    val smallCity: String,
    val mbti: String,
    val job: String,
    val hobby: List<String>,
    val matchingReason: String,
) {
    companion object {
        fun toResponse(member: Member): MemberResponse {
            val profile = member.getProfileOrThrow()
            return MemberResponse(
                memberId = member.getIdOrThrow(),
                codeImage = profile.getCodeImageOrThrow().first(),
                faceImage = profile.getFaceImageOrThrow().first(),
                name = profile.codeName,
                age = profile.age,
                introduce = profile.introduce,
                bigCity = profile.bigCity,
                smallCity = profile.smallCity,
                mbti = profile.mbti,
                job = profile.job,
                hobby = Profile.deserializeAttribute(profile.hobby),
                matchingReason = "mbti"
            )
        }
    }
}
