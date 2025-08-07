package codel.chat.business

import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatRequest
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.SavedChatDto
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.infrastructure.SignalJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val memberRepository: MemberRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val signalJpaRepository: SignalJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
) {
    fun createChatRoom(
        requester: Member,
        request: CreateChatRoomRequest,
    ): ChatRoomResponse {
        val partner = memberRepository.findDoneMember(request.partnerId)
        val savedChatRoom = chatRoomRepository.saveChatRoom(requester, partner)

        return ChatRoomResponse.toResponse(savedChatRoom, requester, 0, partner, 0)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(
        requester: Member,
        pageable: Pageable,
    ): Page<ChatRoomResponse> {
        val pagedChatRooms = chatRoomRepository.findChatRooms(requester, pageable)

        return pagedChatRooms.map { chatRoom ->
            ChatRoomResponse.toResponse(
                chatRoom,
                requester,
                chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)?.lastReadChat?.getIdOrThrow(),
                chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), requester),
                chatRepository.getUnReadMessageCount(chatRoom, requester),
            )
        }
    }

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatRequest: ChatRequest,
    ): SavedChatDto {
        val now = LocalDate.now()
        val recentChatTime = chatRequest.recentChatTime

        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        if(now != recentChatTime.toLocalDate()) {
            val dateMessage = now.toString()
            chatRepository.saveDateChat(chatRoom, dateMessage)
        }
        val savedChat = chatRepository.saveChat(chatRoomId, requester, chatRequest)

        chatRoom.updateRecentChat(savedChat)

        val partner = chatRoomRepository.findPartner(chatRoomId, requester)
        val unReadMessageCount = chatRepository.getUnReadMessageCount(chatRoom, requester)

        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        val chatRoomResponse = ChatRoomResponse.toResponse(chatRoom, requester, savedChat.getIdOrThrow(), partner, unReadMessageCount)

        return SavedChatDto(partner, chatRoomResponse, chatResponse)
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        lastChatId : Long?,
        requester: Member,
        pageable: Pageable,
    ): Page<ChatResponse> {
        val pagedChats = chatRepository.findNextChats(chatRoomId, lastChatId, pageable)
        return pagedChats.map { chat -> ChatResponse.toResponse(requester, chat) }
    }

    fun updateLastChat(
        chatRoomId: Long,
        chatLogRequest: ChatLogRequest,
        requester: Member,
    ) {
        val lastChat = chatRepository.findChat(chatLogRequest.lastChatId)

        chatRepository.upsertLastChat(chatRoomId, requester, lastChat)
    }

    fun updateUnlockChatRoom(requester: Member, chatRoomId: Long) {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)

        chatRoom.unlock(requester.getIdOrThrow())
    }
}
