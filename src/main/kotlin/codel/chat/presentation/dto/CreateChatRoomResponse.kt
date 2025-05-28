package codel.chat.presentation.dto

import codel.chat.domain.ChatRoom

class CreateChatRoomResponse(
    val chatRoomId: Long,
) {
    companion object {
        fun toResponse(chatRoom: ChatRoom): CreateChatRoomResponse =
            CreateChatRoomResponse(
                chatRoomId = chatRoom.id ?: throw IllegalArgumentException("채팅방 id가 없습니다."),
            )
    }
}
