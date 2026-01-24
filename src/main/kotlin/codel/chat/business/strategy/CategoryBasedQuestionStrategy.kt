package codel.chat.business.strategy

import codel.chat.business.ChatService
import codel.chat.presentation.request.QuestionRecommendRequest
import codel.chat.presentation.response.QuestionRecommendResponseV2
import codel.member.domain.Member
import codel.question.business.QuestionRecommendationResult
import codel.question.business.QuestionService
import codel.question.presentation.response.QuestionResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

/**
 * 카테고리 기반 질문 추천 전략 (1.3.0 이상)
 *
 * - 채팅방용 카테고리: 가치관, 텐션업 코드, 만약에 코드, 비밀 코드(19+)
 * - A/B 그룹 정책 적용 (텐션업 제외)
 */
@Component
class CategoryBasedQuestionStrategy(
    private val questionService: QuestionService,
    private val chatService: ChatService
) : QuestionRecommendStrategy {

    override fun recommendQuestion(
        chatRoomId: Long,
        member: Member,
        request: QuestionRecommendRequest
    ): ResponseEntity<Any> {
        // 카테고리 필수 검증
        val category = request.category
            ?: return ResponseEntity.badRequest()
                .body(mapOf("message" to "카테고리를 선택해주세요."))

        // 채팅방용 카테고리 검증
        if (!category.isChatCategory()) {
            return ResponseEntity.badRequest()
                .body(mapOf("message" to "채팅방에서 사용할 수 없는 카테고리입니다."))
        }

        // 카테고리별 정책에 따른 질문 추천
        val result = questionService.recommendQuestionForChat(chatRoomId, category)

        return when (result) {
            is QuestionRecommendationResult.Success -> {
                val savedChat = chatService.sendQuestionMessage(chatRoomId, member, result.question)
                ResponseEntity.ok(
                    QuestionRecommendResponseV2.success(
                        question = QuestionResponse.from(result.question),
                        chat = savedChat
                    )
                )
            }
            is QuestionRecommendationResult.Exhausted -> {
                ResponseEntity.ok(QuestionRecommendResponseV2.exhausted())
            }
        }
    }
}
