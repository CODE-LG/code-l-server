package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.presentation.request.ChatRequest
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.SavedChatDto
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val memberRepository: MemberRepository,
) {
    fun createChatRoom(
        requester: Member,
        request: CreateChatRoomRequest,
    ): ChatRoomResponse {
        val partner = memberRepository.findDoneMember(request.partnerId)

        val savedChatRoom = chatRoomRepository.saveChatRoom()
        chatRoomRepository.saveChatRoomMember(savedChatRoom, requester)
        chatRoomRepository.saveChatRoomMember(savedChatRoom, partner)

        return ChatRoomResponse.toResponse(savedChatRoom, requester, partner, 0)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(
        requester: Member,
        pageable: Pageable,
    ): Page<ChatRoomResponse> {
        val pagedChatRooms = chatRoomRepository.findChatRooms(requester, pageable)

        return pagedChatRooms.map { chatRoom -> convertChatRoomToChatRoomResponse(chatRoom, requester) }
    }

    @Transactional(readOnly = true)
    fun getChatRoom(
        chatRoomId: Long,
        requester: Member,
    ): ChatRoomResponse {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)

        return convertChatRoomToChatRoomResponse(chatRoom, requester)
    }

    private fun convertChatRoomToChatRoomResponse(
        chatRoom: ChatRoom,
        requester: Member,
    ) = ChatRoomResponse.toResponse(
        chatRoom,
        requester,
        chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), requester),
        chatRepository.getUnReadMessageCount(chatRoom, requester),
    )

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatRequest: ChatRequest,
    ): SavedChatDto {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val savedChat = chatRepository.saveChat(chatRoomId, requester, chatRequest)
        val partner = chatRoomRepository.findPartner(chatRoomId, requester)
        val unReadMessageCount = chatRepository.getUnReadMessageCount(chatRoom, requester)

        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        val chatRoomResponse = ChatRoomResponse.toResponse(chatRoom, requester, partner, unReadMessageCount)

        return SavedChatDto(partner, chatRoomResponse, chatResponse)
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        requester: Member,
        pageable: Pageable,
    ): Page<ChatResponse> {
        val pagedChats = chatRepository.findChats(chatRoomId, pageable)

        return pagedChats.map { chat -> ChatResponse.toResponse(requester, chat) }
    }

    fun updateLastChat(
        chatRoomId: Long,
        updateLastChatRequest: UpdateLastChatRequest,
        requester: Member,
    ) {
        val lastChat = chatRepository.findChat(updateLastChatRequest.lastChatId)

        chatRepository.upsertLastChat(chatRoomId, requester, lastChat)
    }
}
