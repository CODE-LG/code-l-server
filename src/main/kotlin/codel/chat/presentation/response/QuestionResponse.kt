package codel.chat.presentation.response

import codel.chat.domain.Question

data class QuestionResponse(
    val questionId: Long,
    val content: String
) {
    companion object {
        fun from(question: Question): QuestionResponse {
            return QuestionResponse(
                questionId = question.getIdOrThrow(),
                content = question.content
            )
        }
    }
}
