package codel.chat.presentation.response

import codel.member.domain.Member

data class SavedChatDto(
    val partner: Member,
    val chatRoomResponse: ChatRoomResponse,
    val chatResponse: ChatResponse,
)
