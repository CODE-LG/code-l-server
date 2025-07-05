package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import java.time.LocalDateTime

data class MemberResponse(
    val memberId: Long,
    val codeImage: String,
    val name: String,
    val age: Int,
    val introduce: String,
    val bigCity: String,
    val smallCity: String,
    val mbti: String,
    val job: String,
    val hobby: List<String>,
    val matchingReason: String,
    val unlockTime: LocalDateTime,
) {
    companion object {
        fun toResponse(member: Member): MemberResponse {
            val profile = member.getProfileOrThrow()
            return MemberResponse(
                memberId = member.getIdOrThrow(),
                codeImage = profile.getCodeImageOrThrow().first(),
                name = profile.codeName,
                age = profile.age,
                introduce = profile.introduce,
                bigCity = profile.bigCity,
                smallCity = profile.smallCity,
                mbti = profile.mbti,
                job = profile.job,
                hobby = Profile.deserializeAttribute(profile.hobby),
                matchingReason = "mbti",
                // FIXME: 코드 해제 시간으로 변경
                unlockTime = LocalDateTime.now(),
            )
        }
    }
}
