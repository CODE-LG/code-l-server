package codel.member.presentation.request

import codel.member.domain.Profile
import kotlin.String

data class ProfileSavedRequest(
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
    val question : String,
    val answer : String,
) {
    fun toProfile(): Profile {
        val serializeHobby = Profile.serializeAttribute(hobby)
        val serializeStyle = Profile.serializeAttribute(style)
        return Profile(
            codeName = this.codeName,
            age = this.age,
            job = this.job,
            alcohol = this.alcohol,
            smoke = this.smoke,
            hobby = serializeHobby,
            style = serializeStyle,
            bigCity = this.bigCity,
            smallCity = this.smallCity,
            mbti = this.mbti,
            introduce = this.introduce,
            question = this.question,
            answer = this.answer,
        )
    }
}
