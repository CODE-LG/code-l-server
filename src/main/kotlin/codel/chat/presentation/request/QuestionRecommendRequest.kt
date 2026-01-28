package codel.chat.presentation.request

import codel.question.domain.QuestionCategory
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 추천 요청")
data class QuestionRecommendRequest(
    @Schema(description = "선택한 카테고리 (1.3.0 이상에서 필수)", required = false)
    val category: QuestionCategory? = null
)
