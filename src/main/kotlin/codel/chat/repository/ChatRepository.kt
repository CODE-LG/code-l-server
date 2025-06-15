package codel.chat.repository

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoomMember
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.LastReadChatJpaRepository
import codel.chat.presentation.request.ChatRequest
import org.springframework.stereotype.Component

@Component
class ChatRepository(
    private val chatJpaRepository: ChatJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val lastReadChatJpaRepository: LastReadChatJpaRepository,
) {
    fun saveChat(
        requester: ChatRoomMember,
        partner: ChatRoomMember,
        chatRequest: ChatRequest,
    ): Chat = chatJpaRepository.save(Chat.of(requester, partner, chatRequest))

    fun getChats(requester: ChatRoomMember): List<Chat> = chatJpaRepository.findByChatRoomOrderBySentAt(requester.chatRoom)

//    fun findLastChat(chatRooms: List<ChatRoom>): Map<ChatRoom, Chat>? {
//    }
}
