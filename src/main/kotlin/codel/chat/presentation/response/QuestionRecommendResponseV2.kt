package codel.chat.presentation.response

import codel.question.presentation.response.QuestionResponse
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 추천 응답 (1.3.0 이상)")
data class QuestionRecommendResponseV2(
    @Schema(description = "추천 성공 여부")
    val success: Boolean,

    @Schema(description = "추천된 질문 (소진 시 null)")
    val question: QuestionResponse?,

    @Schema(description = "생성된 채팅 메시지")
    val chat: SavedChatDto?,

    @Schema(description = "소진 안내 메시지 (소진 시에만)")
    val exhaustedMessage: String?
) {
    companion object {
        fun success(question: QuestionResponse, chat: SavedChatDto) = QuestionRecommendResponseV2(
            success = true,
            question = question,
            chat = chat,
            exhaustedMessage = null
        )

        fun exhausted() = QuestionRecommendResponseV2(
            success = false,
            question = null,
            chat = null,
            exhaustedMessage = "이 채팅방에서는 해당 카테고리 질문을 모두 사용했어요. 다른 카테고리에서 새로운 질문을 추천받아 보세요."
        )
    }
}
