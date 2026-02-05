package codel.question.business

import codel.question.domain.Question

/**
 * 질문 추천 결과
 */
sealed class QuestionRecommendationResult {
    /**
     * 추천 성공
     */
    data class Success(val question: Question) : QuestionRecommendationResult()

    /**
     * 질문 소진 (해당 카테고리의 모든 질문이 사용됨)
     */
    data object Exhausted : QuestionRecommendationResult()
}
