package codel.chat.presentation.response

import codel.member.domain.Member

data class QuestionSendResult(
    val chatResponse: ChatResponse,
    val partner: Member,
    val requesterChatRoomResponse: ChatRoomResponse, // 발송자용 채팅방 응답
    val partnerChatRoomResponse: ChatRoomResponse    // 수신자용 채팅방 응답
)