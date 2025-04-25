package codel.member.infrastructure.entity

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.Profile
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class ProfileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,
    var codeName: String,
    var age: Int,
    var job: String,
    var alcohol: String,
    var smoke: String,
    var hobby: String, // 복수
    var style: String, // 복수
    var bigCity: String,
    var smallCity: String,
    var mbti: String,
    var introduce: String,
    @Column(length = 1000)
    var codeImage: String? = null, // 복수
    @Column(length = 1000)
    var faceImage: String? = null, // 복수
) {
    companion object {
        fun toEntity(profile: Profile): ProfileEntity =
            ProfileEntity(
                codeName = profile.codeName,
                age = profile.age,
                job = profile.job,
                alcohol = profile.alcohol,
                smoke = profile.smoke,
                hobby = serializeAttribute(profile.hobby),
                style = serializeAttribute(profile.style),
                bigCity = profile.bigCity,
                smallCity = profile.smallCity,
                mbti = profile.mbti,
                introduce = profile.introduce,
            )

        private fun serializeAttribute(attribute: List<String>): String = attribute.joinToString(separator = ",")

        private fun deserializeAttribute(attribute: String): List<String> = attribute.split(",")
    }

    fun getCodeImage(): List<String>? = this.codeImage?.let { deserializeAttribute(it) }

    fun getFaceImage(): List<String>? = this.faceImage?.let { deserializeAttribute(it) }

    fun toDomain(): Profile =
        Profile(
            id = this.id,
            codeName = this.codeName,
            age = this.age,
            job = this.job,
            alcohol = this.alcohol,
            smoke = this.smoke,
            hobby = deserializeAttribute(this.hobby),
            style = deserializeAttribute(this.style),
            bigCity = this.bigCity,
            smallCity = this.smallCity,
            mbti = this.mbti,
            introduce = this.introduce,
        )

    fun updateCodeImage(codeImage: CodeImage) {
        this.codeImage = serializeAttribute(codeImage.urls)
    }

    fun updateFaceImage(faceImage: FaceImage) {
        this.faceImage = serializeAttribute(faceImage.urls)
    }
}
