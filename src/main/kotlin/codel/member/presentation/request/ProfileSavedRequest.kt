package codel.member.presentation.request

import codel.member.domain.Profile

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
) {
    fun toProfile(): Profile =
        Profile(
            codeName = this.codeName,
            age = this.age,
            job = this.job,
            alcohol = this.alcohol,
            smoke = this.smoke,
            hobby = this.hobby,
            style = this.style,
            bigCity = this.bigCity,
            smallCity = this.smallCity,
            mbti = this.mbti,
            introduce = this.introduce,
        )
}
