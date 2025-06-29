package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomInfo
import java.time.LocalDateTime

data class ChatRoomResponse(
    val chatRoomId: Long,
    val name: String,
    val mainImageUrl: String,
    val lastMessage: String,
    val lastMessageSentAt: LocalDateTime,
    val unReadMessageCount: Int,
) {
    companion object {
        fun of(
            chatRoom: ChatRoom,
            chatRoomInfo: ChatRoomInfo,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                name =
                    chatRoomInfo.partner.member
                        .getProfileOrThrow()
                        .codeName,
                mainImageUrl =
                    chatRoomInfo.partner.member
                        .getProfileOrThrow()
                        .getFirstCodeImage(),
                lastMessage = chatRoomInfo.recentChat?.message ?: "",
                lastMessageSentAt = chatRoomInfo.recentChat?.sentAt ?: LocalDateTime.MIN,
                unReadMessageCount = chatRoomInfo.unReadMessageCount,
            )
    }
}
