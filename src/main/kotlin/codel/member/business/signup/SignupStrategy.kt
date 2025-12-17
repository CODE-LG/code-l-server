package codel.member.business.signup

import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile

/**
 * 회원가입 히든 이미지 등록 전략 인터페이스
 *
 * 앱 버전과 회원 상태에 따라 다른 동작을 수행하기 위한 전략 패턴
 */
interface SignupStrategy {

    /**
     * 히든 이미지 등록 처리
     *
     * @param member 로그인한 회원
     * @param images 업로드할 이미지 파일 목록
     * @return 처리 결과 응답
     */
    fun handleHiddenImages(
        member: Member,
        images: List<MultipartFile>
    ): ResponseEntity<Any>
}
