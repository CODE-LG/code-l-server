package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import java.time.LocalDateTime

data class ChatRoomResponse(
    val chatRoomId: Long,
    val name: String,
    val mainImageUrl: String,
    val lastMessage: String,
    val lastMessageSentAt: LocalDateTime,
    val unReadMessageCount: Int,
) {
    // TODO. lastMessage, lastMessageSentAt, unReadMessageCount 매핑
    companion object {
        fun of(
            chatRoom: ChatRoom,
            partner: ChatRoomMember,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                name = partner.member.profile?.codeName ?: "",
                mainImageUrl = partner.member.profile?.codeImage ?: "",
                lastMessage = "",
                lastMessageSentAt = LocalDateTime.now(),
                unReadMessageCount = 1,
            )
    }
}
