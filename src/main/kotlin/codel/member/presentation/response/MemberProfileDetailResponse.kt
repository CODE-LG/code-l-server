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

            val deserializeHobby = Profile.deserializeAttribute(profile.hobby)
            val deserializeStyle = Profile.deserializeAttribute(profile.style)
            return MemberProfileDetailResponse(
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
                signalStatus = signalStatus,
                isUnlocked = false,
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }


        fun toResponse(member: Member, signalStatus : SignalStatus, isUnlocked : Boolean): MemberProfileDetailResponse {
            val profile = member.getProfileOrThrow()

            val deserializeHobby = Profile.deserializeAttribute(profile.hobby)
            val deserializeStyle = Profile.deserializeAttribute(profile.style)
            return MemberProfileDetailResponse(
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
                signalStatus = signalStatus,
                isUnlocked = isUnlocked,
                codeImages = profile.getCodeImageOrThrow(),
                faceImages = profile.getFaceImageOrThrow(),
            )
        }
    }
}
