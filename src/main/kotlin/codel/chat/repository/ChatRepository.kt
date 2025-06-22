package codel.chat.repository

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.LastReadChat
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.LastReadChatJpaRepository
import codel.chat.presentation.request.ChatRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRepository(
    private val chatJpaRepository: ChatJpaRepository,
    private val lastReadChatJpaRepository: LastReadChatJpaRepository,
) {
    fun saveChat(
        requester: ChatRoomMember,
        partner: ChatRoomMember,
        chatRequest: ChatRequest,
    ): Chat = chatJpaRepository.save(Chat.of(requester, partner, chatRequest))

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
        val entity =
            lastReadChatJpaRepository
                .findByChatRoomMember(chatRoomMember)
                ?.apply { this.chat = chat }
                ?: LastReadChat(chatRoomMember = chatRoomMember, chat = chat)

        lastReadChatJpaRepository.save(entity)
    }
}
