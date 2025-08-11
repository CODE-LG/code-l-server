package codel.chat.presentation.response

import codel.member.domain.Member

data class QuestionSendResult(
    val chatResponse: ChatResponse,
    val partner: Member,
    val updatedChatRoom: ChatRoomResponse
)
