package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.member.domain.Member
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
            partner: Member,
            unReadMessageCount: Int,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                name = partner.profile?.codeName ?: "",
                mainImageUrl = partner.profile?.codeImage ?: "",
                lastMessage = chatRoom.recentChat?.message ?: "",
                lastMessageSentAt = chatRoom.recentChat?.sentAt ?: LocalDateTime.MIN,
                unReadMessageCount = unReadMessageCount,
            )
    }
}
