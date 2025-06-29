package codel.member.domain

import codel.member.exception.MemberException
import org.springframework.http.HttpStatus

class FaceImage(
    val urls: List<String>,
) {
    init {
        if (urls.size != 3) {
            throw MemberException(HttpStatus.BAD_REQUEST, "얼굴 이미지 URL은 정확히 3개여야 합니다.")
        }
    }

    fun serializeAttribute(): String = urls.joinToString(separator = ",")
}
