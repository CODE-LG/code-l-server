package codel.chat.presentation.response

import codel.question.presentation.response.QuestionResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 추천 응답 (1.3.0 미만, 레거시)")
data class QuestionRecommendResponseLegacy(
    @Schema(description = "추천 성공 여부")
    val success: Boolean,

    @Schema(description = "추천된 질문 (소진 시 null)")
    val question: QuestionResponse?,

    @Schema(description = "메시지")
    val message: String?
) {
    companion object {
        fun success(question: QuestionResponse) = QuestionRecommendResponseLegacy(
            success = true,
            question = question,
            message = null
        )

        fun exhausted() = QuestionRecommendResponseLegacy(
            success = false,
            question = null,
            message = "추천할 수 있는 질문이 없습니다."
        )
    }
}
