package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import java.time.LocalDateTime

data class ChatRoomResponse(
    val roomId: Long,
    val memberId: Long,
    val name: String,
    val mainImageUrl: String,
    val lastMessage: String,
    val lastMessageSentAt: LocalDateTime,
    val unReadMessageCount: Int,
) {
    companion object {
        // TODO. memberEntity 받아서 반화하는 로직 수정
        // TODO. 마지막 채팅, 마지막 채팅 시간, 안읽은 메시지 개수 추가
        fun of(
            chatRoom: ChatRoom,
            partner: ChatRoomMember,
        ): ChatRoomResponse =
            ChatRoomResponse(
                roomId = chatRoom.getIdOrThrow(),
                memberId = partner.memberEntity.getIdOrThrow(),
                name = partner.memberEntity.profileEntity?.codeName ?: "",
                mainImageUrl = partner.memberEntity.profileEntity?.codeImage ?: "",
                lastMessage = "",
                lastMessageSentAt = LocalDateTime.now(),
                unReadMessageCount = 1,
            )
    }
}
