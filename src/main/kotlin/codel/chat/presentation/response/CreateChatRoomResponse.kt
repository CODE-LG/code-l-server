package codel.chat.presentation.response

import codel.chat.domain.ChatRoom

data class CreateChatRoomResponse(
    val chatRoomId: Long,
) {
    companion object {
        fun toResponse(chatRoom: ChatRoom): CreateChatRoomResponse =
            CreateChatRoomResponse(
                chatRoomId = chatRoom.id ?: throw IllegalArgumentException("채팅방 id가 없습니다."),
            )
    }
}
