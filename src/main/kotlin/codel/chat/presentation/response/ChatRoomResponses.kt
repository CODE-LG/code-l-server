package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomInfo

data class ChatRoomResponses(
    val chatrooms: List<ChatRoomResponse>,
) {
    companion object {
        fun of(partnerByChatRoom: Map<ChatRoom, ChatRoomInfo>): ChatRoomResponses =
            ChatRoomResponses(
                chatrooms =
                    partnerByChatRoom.map { entry ->
                        ChatRoomResponse.of(
                            entry.key,
                            entry.value,
                        )
                    },
            )
    }
}
