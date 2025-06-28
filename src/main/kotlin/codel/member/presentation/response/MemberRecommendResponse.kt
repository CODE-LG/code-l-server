package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import codel.member.exception.MemberException
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

data class MemberRecommendResponse(
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
        fun toResponse(member: Member): MemberRecommendResponse {
            val profile = member.profile ?: throw MemberException(HttpStatus.BAD_REQUEST, "멤버의 프로필 정보가 없습니다.")
            return MemberRecommendResponse(
                memberId = member.getIdOrThrow(),
                codeImage =
                    profile.getCodeImage()?.firstOrNull()
                        ?: throw MemberException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "프로필 이미지가 존재하지 않습니다",
                        ),
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
