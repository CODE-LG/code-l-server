package codel.chat.presentation.response

import codel.chat.domain.ChatRoom
import codel.member.domain.Member
import codel.member.presentation.response.MemberRecommendResponse
import java.time.LocalDateTime

data class ChatRoomResponse(
    val chatRoomId: Long,
    val unReadMessageCount: Int,
    val partner: MemberRecommendResponse,
    val recentChat: ChatResponse?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun toResponse(
            chatRoom: ChatRoom,
            requester: Member,
            partner: Member,
            unReadMessageCount: Int,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                partner = MemberRecommendResponse.toResponse(partner),
                recentChat = chatRoom.recentChat?.let { ChatResponse.toResponse(requester, it) },
                unReadMessageCount = unReadMessageCount,
                createdAt = chatRoom.createdAt,
                updatedAt = chatRoom.updatedAt,
            )
    }
}
