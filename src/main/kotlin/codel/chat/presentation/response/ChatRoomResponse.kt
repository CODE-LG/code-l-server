package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomStatus
import codel.member.domain.Member
import codel.member.presentation.response.MemberResponse
import java.time.LocalDateTime

data class ChatRoomResponse(
    val chatRoomId: Long,
    val unReadMessageCount: Int,
    val partner: MemberResponse,
    val lastReadChatId : Long?,
    val recentChat: ChatResponse?,
    val chatRoomStatus : ChatRoomStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun toResponse(
            chatRoom: ChatRoom,
            requester: Member,
            lastReadChatId: Long?,
            partner: Member,
            unReadMessageCount: Int,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                partner = MemberResponse.toResponse(partner),
                lastReadChatId = lastReadChatId,
                recentChat = chatRoom.recentChat?.let { ChatResponse.toResponse(requester, it) },
                unReadMessageCount = unReadMessageCount,
                chatRoomStatus = chatRoom.status,
                createdAt = chatRoom.createdAt,
                updatedAt = chatRoom.updatedAt,
            )
    }
}
