package codel.member.domain

import codel.member.exception.MemberException
import jakarta.persistence.*
import org.springframework.http.HttpStatus

@Entity
class Profile(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
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
    var question: String,
    var answer: String,
    @Column(length = 1000)
    var codeImage: String? = null, // 복수
    @Column(length = 1000)
    var faceImage: String? = null, // 복수
    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "member_id")
    var member: Member? = null,
) {
    companion object {
        fun serializeAttribute(attribute: List<String>): String = attribute.joinToString(separator = ",")

        fun deserializeAttribute(attribute: String): List<String> = attribute.split(",")
    }

    fun getCodeImageOrThrow(): List<String> =
        this.codeImage?.let { deserializeAttribute(it) }
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "코드 이미지가 존재하지 않습니다.")

    fun getFaceImageOrThrow(): List<String> =
        this.faceImage?.let { deserializeAttribute(it) }
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "페이스 이미지가 존재하지 않습니다.")

    fun update(updateProfile: Profile) {
        this.codeImage = updateProfile.codeImage
        this.faceImage = updateProfile.faceImage
        this.codeName = updateProfile.codeName
        this.age = updateProfile.age
        this.job = updateProfile.job
        this.smoke = updateProfile.smoke
        this.hobby = updateProfile.hobby
        this.style = updateProfile.style
        this.bigCity = updateProfile.bigCity
        this.question = updateProfile.question
        this.answer = updateProfile.answer
        this.introduce = updateProfile.introduce
    }

    fun registerCodeImage(codeImage: String) {

    }
}
