package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.presentation.request.ChatRequest
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.ChatRoomResponses
import codel.chat.presentation.response.CreateChatRoomResponse
import codel.chat.presentation.response.SavedChatDto
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
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
    ): CreateChatRoomResponse {
        val partner = memberRepository.findMember(request.partnerId)
        if (partner.isNotDone()) {
            throw IllegalArgumentException("상대방 멤버가 회원가입을 완료하지 않았습니다.")
        }

        val savedChatRoom = chatRoomRepository.saveChatRoom()
        chatRoomRepository.saveChatRoomMembers(savedChatRoom, requester, partner)

        return CreateChatRoomResponse.toResponse(savedChatRoom)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(requester: Member): ChatRoomResponses {
        val requesterChatRoomMembers = chatRoomRepository.findChatRoomsByMember(requester)
        val chatRoomMemberByChatRoom: Map<ChatRoom, ChatRoomMember> =
            requesterChatRoomMembers.associate { chatRoomMember ->
                val chatRoom = chatRoomMember.chatRoom
                chatRoom to chatRoomRepository.findPartner(chatRoom, chatRoomMember.member)
            }

        return ChatRoomResponses.of(chatRoomMemberByChatRoom)
    }

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatRequest: ChatRequest,
    ): SavedChatDto {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoom, requester)
        val partnerChatRoomMember = chatRoomRepository.findPartner(chatRoom, requester)

        val savedChat = chatRepository.saveChat(requesterChatRoomMember, partnerChatRoomMember, chatRequest)

        return SavedChatDto(
            partner = partnerChatRoomMember.member,
            chatRoomResponse = getChatRoomResponse(requester, chatRoom),
            chatResponse = ChatResponse.of(savedChat, requester),
        )
    }

    private fun getChatRoomResponse(
        requester: Member,
        chatRoom: ChatRoom,
    ): ChatRoomResponse {
        val partnerChatRoomMember = chatRoomRepository.findPartner(chatRoom, requester)

        return ChatRoomResponse.of(chatRoom, partnerChatRoomMember)
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        requester: Member,
    ): ChatResponses {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoom, requester)

        val chats = chatRepository.findChats(requesterChatRoomMember)
        return ChatResponses.of(chats, requester)
    }

    @Transactional
    fun updateLastChat(
        chatRoomId: Long,
        updateLastChatRequest: UpdateLastChatRequest,
        requester: Member,
    ) {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoom, requester)
        val lastChat = chatRepository.findChat(updateLastChatRequest.lastChatId)

        chatRepository.upsertLastChat(requesterChatRoomMember, lastChat)
    }
}
