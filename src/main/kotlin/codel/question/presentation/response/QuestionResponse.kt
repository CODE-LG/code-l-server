package codel.question.presentation.response

import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 응답 정보")
data class QuestionResponse(
    @Schema(description = "질문 고유 ID", example = "1")
    val questionId: Long,
    
    @Schema(description = "질문 카테고리", example = "VALUES")
    val category : QuestionCategory,
    
    @Schema(description = "질문 내용", example = "당신의 인생에서 가장 중요한 가치는 무엇인가요?")
    val content: String
) {
    companion object {
        fun from(question: Question): QuestionResponse {
            return QuestionResponse(
                questionId = question.getIdOrThrow(),
                category = question.category,
                content = question.content
            )
        }
    }
}