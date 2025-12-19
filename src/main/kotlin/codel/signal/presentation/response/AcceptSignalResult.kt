package codel.signal.presentation.response

import codel.chat.presentation.response.ChatRoomResponse
import codel.member.domain.Member

data class AcceptSignalResult(
    val approverChatRoomResponse: ChatRoomResponse,
    val partnerChatRoomResponse: ChatRoomResponse,
    val partner: Member
)
