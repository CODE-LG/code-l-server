package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.presentation.request.ChatRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.ChatRoomResponses
import codel.chat.presentation.response.CreateChatRoomResponse
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.MemberRepository
import codel.member.infrastructure.entity.MemberEntity
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
        requester: MemberEntity,
        partnerId: Long,
    ): CreateChatRoomResponse {
        val partner = memberRepository.findMember(partnerId)
        if (partner.isNotDone()) {
            throw IllegalArgumentException("상대방 멤버가 회원가입을 완료하지 않았습니다.")
        }

        val savedChatRoom = chatRoomRepository.createChatRoom()
        chatRoomRepository.saveChatRoomMembers(savedChatRoom, requester, partner)

        return CreateChatRoomResponse.toResponse(savedChatRoom)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(requester: MemberEntity): ChatRoomResponses {
        val requesterChatRoomMembers = chatRoomRepository.findChatRoomsByMember(requester)
        val chatRoomMemberByChatRoom: Map<ChatRoom, ChatRoomMember> =
            requesterChatRoomMembers.associate { chatRoomMember ->
                val chatRoom = chatRoomMember.chatRoom
                chatRoom to chatRoomRepository.findPartner(chatRoom, chatRoomMember.memberEntity)
            }

        return ChatRoomResponses.of(chatRoomMemberByChatRoom)
    }

    fun saveChat(
        chatRoomId: Long,
        requester: MemberEntity,
        chatRequest: ChatRequest,
    ): Pair<ChatRoomResponse, ChatResponse> {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoom, requester)
        val partnerChatRoomMember = chatRoomRepository.findPartner(chatRoom, requester)

        val savedChat = chatRepository.saveChat(requesterChatRoomMember, partnerChatRoomMember, chatRequest)

        return Pair(getChatRoomResponse(requester, chatRoom), ChatResponse.of(savedChat, requester))
    }

    private fun getChatRoomResponse(
        requester: MemberEntity,
        chatRoom: ChatRoom,
    ): ChatRoomResponse {
        val partnerChatRoomMember = chatRoomRepository.findPartner(chatRoom, requester)

        return ChatRoomResponse.of(chatRoom, partnerChatRoomMember)
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        requester: MemberEntity,
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
        requester: MemberEntity,
    ) {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoom, requester)
        val lastChat = chatRepository.findChat(updateLastChatRequest.lastChatId)

        chatRepository.upsertLastChat(requesterChatRoomMember, lastChat)
    }
}
