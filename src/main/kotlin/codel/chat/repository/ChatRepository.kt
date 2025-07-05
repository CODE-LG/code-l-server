package codel.chat.repository

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatRoom_
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatRequest
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRepository(
    private val chatJpaRepository: ChatJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
) {
    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatRequest: ChatRequest,
    ): Chat {
        val requesterChatRoomMember = findMe(chatRoomId, requester)

        return chatJpaRepository.save(Chat.of(requesterChatRoomMember, chatRequest))
    }

    fun findChats(
        chatRoomId: Long,
        pageable: Pageable,
    ): Page<Chat> {
        val pageableWithSort: Pageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, getChatDefaultSort())
        val chatRoom =
            chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: throw ChatException(
                HttpStatus.BAD_REQUEST,
                "채팅방을 찾을 수 없습니다.",
            )

        return chatJpaRepository.findAllByFromChatRoom(chatRoom, pageableWithSort)
    }

    fun findChat(chatId: Long): Chat =
        chatJpaRepository.findByIdOrNull(chatId) ?: throw ChatException(
            HttpStatus.BAD_REQUEST,
            "해당 chatId에 맞는 채팅을 찾을 수 없습니다.",
        )

    fun upsertLastChat(
        chatRoomId: Long,
        requester: Member,
        chat: Chat,
    ) {
        val requesterChatRoomMember = findMe(chatRoomId, requester)
        requesterChatRoomMember.lastReadChat = chat
        chatRoomMemberJpaRepository.save(requesterChatRoomMember)
    }

    fun getUnReadMessageCount(
        chatRoom: ChatRoom,
        requester: Member,
    ): Int {
        val requesterChatRoomMember =
            chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)
                ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방에 속해있는 사용자가 아닙니다.")

        val lastChat = requesterChatRoomMember.lastReadChat ?: return 0
        return chatJpaRepository.countByChatRoomAfterLastChat(
            chatRoom,
            lastChat.getSentAtOrThrow(),
        )
    }

    private fun findMe(
        chatRoomId: Long,
        requester: Member,
    ): ChatRoomMember =
        chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, requester)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방 멤버가 존재하지 않습니다.")

    private fun getChatDefaultSort(): Sort = Sort.by(Sort.Order.desc(ChatRoom_.CREATED_AT))
}
