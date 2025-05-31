package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomRepository
import codel.chat.presentation.dto.CreateChatRoomResponse
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val memberRepository: MemberRepository,
) {
    fun createChatRoom(
        requester: Member,
        partnerId: Long,
    ): CreateChatRoomResponse {
        val partner = memberRepository.findMember(partnerId)
        if (partner.isNotDone()) {
            throw IllegalArgumentException("상대방 멤버가 회원가입을 완료하지 않았습니다.")
        }

        val chatRoom = ChatRoom()
        val savedChatRoom = chatRoomRepository.saveChatRoom(chatRoom, requester, partner)

        return CreateChatRoomResponse.toResponse(savedChatRoom)
    }
}
