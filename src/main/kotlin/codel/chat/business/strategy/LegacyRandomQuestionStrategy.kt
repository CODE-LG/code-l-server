package codel.chat.business.strategy

import codel.chat.business.ChatService
import codel.chat.presentation.request.QuestionRecommendRequest
import codel.chat.presentation.response.QuestionRecommendResponseLegacy
import codel.member.domain.Member
import codel.question.business.QuestionService
import codel.question.presentation.response.QuestionResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/**
 * 기존 랜덤 질문 추천 전략 (1.3.0 미만)
 *
 * - 카테고리 구분 없이 미사용 질문에서 랜덤 추천
 * - 기존 API 응답 형식 유지
 */
@Component
class LegacyRandomQuestionStrategy(
    private val questionService: QuestionService,
    private val chatService: ChatService
) : QuestionRecommendStrategy {

    override fun recommendQuestion(
        chatRoomId: Long,
        member: Member,
        request: QuestionRecommendRequest
    ): ResponseEntity<Any> {
        // 기존 로직: 카테고리 무관하게 랜덤 질문 추천
        val unusedQuestions = questionService.findUnusedQuestionsByChatRoom(chatRoomId)

        if (unusedQuestions.isEmpty()) {
            return ResponseEntity.ok(QuestionRecommendResponseLegacy.exhausted())
        }

        val selectedQuestion = unusedQuestions.random()
        chatService.sendQuestionMessage(chatRoomId, member, selectedQuestion)

        return ResponseEntity.ok(
            QuestionRecommendResponseLegacy.success(
                question = QuestionResponse.from(selectedQuestion)
            )
        )
    }
}
