package codel.chat.presentation.response

import codel.member.domain.Member

data class SavedChatDto(
    val partner: Member,
    val requesterChatRoomResponse: ChatRoomResponse,
    val partnerChatRoomResponse: ChatRoomResponse,
    val chatResponse: ChatResponse,
)
