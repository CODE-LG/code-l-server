package codel.question.presentation.response

import codel.question.domain.Question
import codel.question.domain.QuestionCategory

data class QuestionResponse(
    val questionId: Long,
    val category : QuestionCategory,
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