package codel.chat.business.strategy

import codel.chat.presentation.request.QuestionRecommendRequest
import codel.member.domain.Member
import org.springframework.http.ResponseEntity

/**
 * 채팅방 질문 추천 전략 인터페이스
 *
 * 앱 버전에 따라 다른 추천 로직을 적용하기 위한 Strategy 패턴
 */
interface QuestionRecommendStrategy {

    /**
     * 질문 추천 처리
     *
     * @param chatRoomId 채팅방 ID
     * @param member 요청한 회원
     * @param request 추천 요청 (카테고리 등)
     * @return 추천 결과 응답
     */
    fun recommendQuestion(
        chatRoomId: Long,
        member: Member,
        request: QuestionRecommendRequest
    ): ResponseEntity<Any>
}
