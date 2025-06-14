package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.presentation.request.CreateChatRoomResponse
import codel.chat.presentation.response.ChatRoomResponses
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.MemberRepository
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val memberRepository: MemberRepository,
) {
    @Transactional
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

    fun getChatRooms(requester: MemberEntity): ChatRoomResponses {
        val requesterChatRoomMembers = chatRoomRepository.findChatRoomsByMember(requester)
        val chatRoomMemberByChatRoom: Map<ChatRoom, ChatRoomMember> =
            requesterChatRoomMembers.associate { chatRoomMember ->
                val chatRoom = chatRoomMember.chatRoom
                chatRoom to chatRoomRepository.findPartner(chatRoom, chatRoomMember.memberEntity)
            }

        return ChatRoomResponses.of(chatRoomMemberByChatRoom)
    }
}
