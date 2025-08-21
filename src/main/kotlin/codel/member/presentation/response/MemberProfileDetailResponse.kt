package codel.member.presentation.response

import codel.member.domain.Member
import codel.member.domain.Profile
import codel.signal.domain.SignalStatus

data class MemberProfileDetailResponse(
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
    val signalStatus : SignalStatus,
    val isUnlocked: Boolean,
    val codeImages: List<String>,
    val faceImages: List<String>
) {
    companion object {
        fun toResponse(member: Member, signalStatus : SignalStatus): MemberProfileDetailResponse {
            val profile = member.getProfileOrThrow()

            return MemberProfileDetailResponse(
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
                question = profile.getQuestionOrThrow(),
                answer = profile.getAnswerOrThrow(),
                signalStatus = signalStatus,
                isUnlocked = false,
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }


        fun toResponse(member: Member, signalStatus : SignalStatus, isUnlocked : Boolean): MemberProfileDetailResponse {
            val profile = member.getProfileOrThrow()

            return MemberProfileDetailResponse(
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
                question = profile.getQuestionOrThrow(),
                answer = profile.getAnswerOrThrow(),
                signalStatus = signalStatus,
                isUnlocked = isUnlocked,
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }
    }
}
