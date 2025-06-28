package codel.member.domain

import codel.member.exception.MemberException
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
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
    @Column(length = 1000)
    var codeImage: String? = null, // 복수
    @Column(length = 1000)
    var faceImage: String? = null, // 복수
    @OneToOne
    @JoinColumn(name = "member_id")
    var member: Member? = null,
) {
    companion object {
        fun serializeAttribute(attribute: List<String>): String = attribute.joinToString(separator = ",")

        fun deserializeAttribute(attribute: String): List<String> = attribute.split(",")
    }

    fun getCodeImage(): List<String>? = this.codeImage?.let { deserializeAttribute(it) }

    fun getFaceImage(): List<String>? = this.faceImage?.let { deserializeAttribute(it) }

    fun getFirstCodeImage(): String {
        val codeImages = getCodeImage() ?: throw MemberException(HttpStatus.BAD_REQUEST, "코드 이미지를 동록하지 않은 사용지압니다.")
        return codeImages.first()
    }
}
