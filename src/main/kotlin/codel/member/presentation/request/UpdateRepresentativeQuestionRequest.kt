package codel.member.presentation.request

/**
 * 대표 질문 및 답변 수정 요청
 */
data class UpdateRepresentativeQuestionRequest(
    val representativeQuestionId: Long,
    val representativeAnswer: String
) {
    init {
        require(representativeAnswer.isNotBlank()) { "대표 답변은 비어있을 수 없습니다" }
        require(representativeAnswer.length <= 1000) { "대표 답변은 1000자를 초과할 수 없습니다" }
    }
}
