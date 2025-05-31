package codel.chat.presentation.dto

import codel.chat.domain.ChatRoom
import codel.member.domain.Member
import java.time.LocalDateTime

class ChatRoomResponse(
    val roomId: Long,
    val memberId: Long,
    val name: String,
    val mainImageUrl: String,
    val lastMessage: String,
    val lastMessageSentAt: LocalDateTime,
    val unReadMessageCount: Int,
) {
    companion object {
        // TODO. 값을 깔끔하게 가져올 수 있는 방법 없을까?
        fun of(
            chatRoom: ChatRoom,
            partner: Member,
        ): ChatRoomResponse =
            ChatRoomResponse(
                roomId = chatRoom.getIdOrThrow(),
                memberId = partner.getIdOrThrow(),
                name = partner.profile?.codeName ?: "",
                mainImageUrl = partner.codeImage?.urls?.get(0) ?: "",
                lastMessage = chatRoom.lastMessage,
                lastMessageSentAt = LocalDateTime.now(),
                unReadMessageCount = chatRoom.unreadMessagesCount,
            )
    }
}
