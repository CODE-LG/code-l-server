package codel.chat.repository

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMemberStatus
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.infrastructure.ChatRoomWithMemberInfo
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRoomRepository(
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
) {
    fun findChatRooms(
        member: Member,
        pageable: Pageable,
    ): Page<ChatRoom> {
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, getChatRoomDefaultSort())

        return chatRoomJpaRepository.findMyChatRoomWithPageable(member.getIdOrThrow(), pageableWithSort)
    }

    /**
     * 활성 상태인 채팅방만 조회 (새로운 메서드)
     */
    fun findActiveChatRoomsByMember(
        member: Member,
        pageable: Pageable,
    ): Page<ChatRoomWithMemberInfo> {
        val pageableWithSort = PageRequest.of(pageable.pageNumber, pageable.pageSize, getChatRoomDefaultSort())
        
        // 사용자가 ACTIVE 상태인 채팅방들을 조회
        val activeChatRoomMembers = chatRoomMemberJpaRepository.findByMemberAndMemberStatus(
            member, 
            ChatRoomMemberStatus.ACTIVE, 
            pageableWithSort
        )
        
        return activeChatRoomMembers.map { chatRoomMember ->
            val partner = findPartner(chatRoomMember.chatRoom.getIdOrThrow(), member)
            val partnerChatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(
                chatRoomMember.chatRoom.getIdOrThrow(), 
                partner
            )
            
            ChatRoomWithMemberInfo(
                chatRoom = chatRoomMember.chatRoom,
                requesterChatRoomMember = chatRoomMember,
                partner = partner,
                partnerChatRoomMember = partnerChatRoomMember
            )
        }
    }

    fun findPartner(
        chatRoomId: Long,
        requester: Member,
    ): Member {
        val chatRoomMember = (
                chatRoomMemberJpaRepository.findByChatRoomIdAndMemberNot(chatRoomId, requester)
                    ?: throw ChatException(HttpStatus.BAD_REQUEST, "채팅방에 자신을 제외한 다른 사용자가 존재하지 않습니다.")
                )

        return chatRoomMember.member
    }

    fun findChatRoomById(chatRoomId: Long): ChatRoom =
        chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: throw ChatException(
            HttpStatus.BAD_REQUEST,
            "chatId에 해당하는 채팅방을 찾을 수 없습니다.",
        )

    private fun getChatRoomDefaultSort(): Sort = Sort.by(Sort.Order.desc("updatedAt"))
}
