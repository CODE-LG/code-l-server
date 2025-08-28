package codel.chat.presentation.response

import codel.chat.business.UnlockInfo
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomStatus
import codel.member.domain.Member
import codel.member.presentation.response.FullProfileResponse
import java.time.LocalDateTime

data class ChatRoomResponse(
    val chatRoomId: Long,
    val unReadMessageCount: Int,
    val partner: FullProfileResponse,
    val lastReadChatId: Long?,
    val recentChat: ChatResponse?,
    val chatRoomStatus: ChatRoomStatus,
    val unlockInfo: UnlockInfoResponse,
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
                partner = FullProfileResponse.createOpen(partner), // 기본적으로는 Open만
                lastReadChatId = lastReadChatId,
                recentChat = chatRoom.recentChat?.let { ChatResponse.toResponse(requester, it) },
                unReadMessageCount = unReadMessageCount,
                chatRoomStatus = chatRoom.status,
                unlockInfo = UnlockInfoResponse(false, null, false), // 임시 - ChatService에서 수정 예정
                createdAt = chatRoom.createdAt,
                updatedAt = chatRoom.updatedAt,
            )

        /**
         * 상대방 상태를 포함한 새로운 응답 생성 메서드
         */
        fun toResponseWithMemberStatus(
            chatRoom: ChatRoom,
            requester: Member,
            lastReadChatId: Long?,
            partner: Member,
            unReadMessageCount: Int,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                partner = FullProfileResponse.createOpen(partner), // 기본적으로는 Open만
                lastReadChatId = lastReadChatId,
                recentChat = chatRoom.recentChat?.let { ChatResponse.toResponse(requester, it) },
                unReadMessageCount = unReadMessageCount,
                chatRoomStatus = chatRoom.status,
                unlockInfo = UnlockInfoResponse(false, null, false), // 임시 - ChatService에서 수정 예정
                createdAt = chatRoom.createdAt,
                updatedAt = chatRoom.updatedAt,
            )

        /**
         * unlockInfo를 포함한 새로운 응답 생성 메서드 (1단계 전용)
         */
        fun toResponseWithUnlockInfo(
            chatRoom: ChatRoom,
            requester: Member,
            lastReadChatId: Long?,
            partner: Member,
            unReadMessageCount: Int,
            unlockInfo: UnlockInfo,
        ): ChatRoomResponse =
            ChatRoomResponse(
                chatRoomId = chatRoom.getIdOrThrow(),
                partner = if (unlockInfo.isUnlocked) {
                    FullProfileResponse.createFull(partner) // 코드 해제된 경우 Full Profile
                } else {
                    FullProfileResponse.createOpen(partner) // 그렇지 않으면 Open만
                },
                lastReadChatId = lastReadChatId,
                recentChat = chatRoom.recentChat?.let { ChatResponse.toResponse(requester, it) },
                unReadMessageCount = unReadMessageCount,
                chatRoomStatus = chatRoom.status,
                unlockInfo = UnlockInfoResponse.from(unlockInfo),
                createdAt = chatRoom.createdAt,
                updatedAt = chatRoom.updatedAt,
            )
    }
}
