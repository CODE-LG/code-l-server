package codel.chat.repository

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRepository(
    private val chatJpaRepository: ChatJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
) {
    fun saveChat(
        requester: ChatRoomMember,
        chatRequest: ChatRequest,
    ): Chat = chatJpaRepository.save(Chat.of(requester, chatRequest))

    fun findChats(requester: ChatRoomMember): List<Chat> = chatJpaRepository.findByFromChatRoomOrderBySentAt(requester.chatRoom)

    fun findChat(chatId: Long): Chat =
        chatJpaRepository.findByIdOrNull(chatId) ?: throw ChatException(
            HttpStatus.BAD_REQUEST,
            "해당 chatId에 맞는 채팅을 찾을 수 없습니다.",
        )

    fun upsertLastChat(
        chatRoomMember: ChatRoomMember,
        chat: Chat,
    ) {
        chatRoomMember.lastChat = chat
        chatRoomMemberJpaRepository.save(chatRoomMember)
    }

    fun getRecentChatByChatRoom(chatRooms: List<ChatRoom>): Map<ChatRoom, Chat?> =
        chatJpaRepository.findRecentChatByChatRooms(chatRooms).associateBy {
            it.chatRoom
        }

    fun getUnReadMessageCountByChatRoom(requesterChatRoomMemberByChatRoom: Map<ChatRoom, ChatRoomMember>): Map<ChatRoom, Int> =
        requesterChatRoomMemberByChatRoom.mapValues { (chatRoom, member) ->
            val lastChat = member.lastChat
            lastChat?.let { chatJpaRepository.countByChatRoomAfterLastChat(chatRoom, it.getSentAtOrThrow()) } ?: 0
        }
}
