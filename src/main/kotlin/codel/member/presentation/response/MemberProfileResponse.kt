package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

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

            return MemberProfileResponse(
                memberId = member.getIdOrThrow(),
                codeName = profile.getCodeNameOrThrow(),
                age = profile.getAge(),
                job = profile.getJobOrThrow(),
                alcohol = profile.getAlcoholOrThrow(),
                smoke = profile.getSmokeOrThrow(),
                hobby = profile.getInterestsList(),
                style = profile.getStylesList(),
                bigCity = profile.getBigCityOrThrow(),
                smallCity = profile.getSmallCityOrThrow(),
                mbti = profile.getMbtiOrThrow(),
                introduce = profile.introduce ?: "",
                question = profile.getRepresentativeQuestionOrThrow().content,
                answer = profile.getRepresentativeAnswerOrThrow(),
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow()
            )
        }

        fun toResponse(member: Member, signalStatus : SignalStatus): MemberProfileResponse {
            val profile = member.getProfileOrThrow()

            return MemberProfileResponse(
                memberId = member.getIdOrThrow(),
                codeName = profile.getCodeNameOrThrow(),
                age = profile.getAge(),
                job = profile.getJobOrThrow(),
                alcohol = profile.getAlcoholOrThrow(),
                smoke = profile.getSmokeOrThrow(),
                hobby = profile.getInterestsList(),
                style = profile.getStylesList(),
                bigCity = profile.getBigCityOrThrow(),
                smallCity = profile.getSmallCityOrThrow(),
                mbti = profile.getMbtiOrThrow(),
                introduce = profile.introduce ?: "",
                question = profile.getRepresentativeQuestionOrThrow().content,
                answer = profile.getRepresentativeAnswerOrThrow(),
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }
    }
}
