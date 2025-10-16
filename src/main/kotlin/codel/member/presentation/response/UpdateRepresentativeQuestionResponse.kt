package codel.member.presentation.response

/**
 * 대표 질문 수정 응답
 */
data class UpdateRepresentativeQuestionResponse(
    val representativeQuestion: QuestionInfo,
    val representativeAnswer: String,
    val message: String
) {
    data class QuestionInfo(
        val id: Long,
        val content: String,
        val category: String
    )
}
