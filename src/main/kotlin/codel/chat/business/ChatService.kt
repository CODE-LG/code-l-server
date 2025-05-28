package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomRepository
import codel.chat.presentation.dto.CreateChatRoomResponse
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
) {
    fun createChatRoom(
        creatorId: Long,
        partnerId: Long,
    ): CreateChatRoomResponse {
        val chatRoom = ChatRoom()
        val savedChatRoom = chatRoomRepository.saveChatRoom(chatRoom, creatorId, partnerId)

        return CreateChatRoomResponse.toResponse(savedChatRoom)
    }
}
