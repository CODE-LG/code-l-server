package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomInfo
import codel.chat.presentation.request.ChatRequest
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.ChatRoomResponses
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
    ): ChatRoomResponse {
        val partner = memberRepository.findDoneMember(request.partnerId)

        val savedChatRoom = chatRoomRepository.saveChatRoom()
        chatRoomRepository.saveChatRoomMember(savedChatRoom, requester)
        val partnerChatRoomMember = chatRoomRepository.saveChatRoomMember(savedChatRoom, partner)

        return ChatRoomResponse.of(savedChatRoom, ChatRoomInfo(partnerChatRoomMember, null, 0))
    }

    @Transactional(readOnly = true)
    fun getChatRooms(requester: Member): ChatRoomResponses {
        val requesterChatRoomMembers = chatRoomRepository.findAllChatRoomMembers(requester)
        val chatRoomMemberByChatRoom: Map<ChatRoom, ChatRoomInfo> =
            requesterChatRoomMembers.associate { chatRoomMember ->
                val chatRoom = chatRoomMember.chatRoom
                chatRoom to
                    ChatRoomInfo(
                        partner = chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), chatRoomMember.member),
                        recentChat = chatRepository.getRecentChat(chatRoom),
                        unReadMessageCount = chatRepository.getUnReadMessageCount(chatRoom, chatRoomMember),
                    )
            }

        return ChatRoomResponses.of(chatRoomMemberByChatRoom)
    }

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatRequest: ChatRequest,
    ): SavedChatDto {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoomId, requester)
        val partnerChatRoomMember = chatRoomRepository.findPartner(chatRoomId, requester)

        val savedChat = chatRepository.saveChat(requesterChatRoomMember, chatRequest)

        return SavedChatDto(
            partner = partnerChatRoomMember.member,
            chatRoomResponse = ChatRoomResponse.of(chatRoom, ChatRoomInfo(partnerChatRoomMember, savedChat, 1)),
            chatResponse = ChatResponse.of(requester, savedChat),
        )
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        requester: Member,
    ): ChatResponses {
        val requesterInChatRoom = chatRoomRepository.findMe(chatRoomId, requester)
        val partnerInChatRoom = chatRoomRepository.findPartner(chatRoomId, requester)

        val chats = chatRepository.findChats(requesterInChatRoom)
        return ChatResponses.of(requester, partnerInChatRoom.member, chats)
    }

    fun updateLastChat(
        chatRoomId: Long,
        updateLastChatRequest: UpdateLastChatRequest,
        requester: Member,
    ) {
        val requesterChatRoomMember = chatRoomRepository.findMe(chatRoomId, requester)
        val lastChat = chatRepository.findChat(updateLastChatRequest.lastChatId)

        chatRepository.upsertLastChat(requesterChatRoomMember, lastChat)
    }
}
