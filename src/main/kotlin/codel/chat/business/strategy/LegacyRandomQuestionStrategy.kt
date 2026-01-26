package codel.chat.business.strategy

import codel.chat.business.ChatService
import codel.chat.presentation.request.QuestionRecommendRequest
import codel.chat.presentation.response.QuestionSendResult
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * 기존 랜덤 질문 추천 전략 (1.3.0 미만)
 *
 * - 카테고리 구분 없이 미사용 질문에서 랜덤 추천
 * - 기존 API 응답 형식 유지 (ChatResponse)
 */
@Component
@Transactional
class LegacyRandomQuestionStrategy(
    private val chatService: ChatService
) : QuestionRecommendStrategy {

    override fun recommendQuestion(
        chatRoomId: Long,
        member: Member,
        request: QuestionRecommendRequest
    ): ResponseEntity<Any> {
        val result = chatService.sendRandomQuestion(chatRoomId, member)
        return ResponseEntity.ok(result)
    }
}
