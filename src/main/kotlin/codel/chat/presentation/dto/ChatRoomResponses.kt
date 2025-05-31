package codel.chat.presentation.dto

import codel.chat.domain.ChatRoom
import codel.member.domain.Member

class ChatRoomResponses(
    val chatrooms: List<ChatRoomResponse>,
) {
    companion object {
        fun of(partnerByChatRoom: Map<ChatRoom, Member>): ChatRoomResponses =
            ChatRoomResponses(
                chatrooms = partnerByChatRoom.map { entry -> ChatRoomResponse.of(entry.key, entry.value) },
            )
    }
}
