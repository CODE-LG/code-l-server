package codel.member.domain

import codel.member.exception.MemberException
import org.springframework.http.HttpStatus

class CodeImage(
    val urls: List<String>,
) {
    init {
        require(urls.size in 1..3) {
            throw MemberException(HttpStatus.BAD_REQUEST, "코드 이미지 URL은 1개 이상 3개 이하이어야 합니다.")
        }
    }
}
