package codel.chat.repository

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class ChatRoomRepository(
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
) {
    fun createChatRoom(): ChatRoom {
        val chatRoom = ChatRoom()
        return chatRoomJpaRepository.save(chatRoom)
    }

    fun saveChatRoomMembers(
        chatRoom: ChatRoom,
        requester: MemberEntity,
        partner: Member,
    ) {
        saveChatRoomMember(chatRoom, requester)
        saveChatRoomMember(chatRoom, findMemberEntity(partner))
    }

    // TODO. Member 리펙토링 이후에 삭제
    private fun findMemberEntity(member: Member) =
        memberJpaRepository.findById(member.getIdOrThrow()).orElseThrow { IllegalArgumentException() }

    private fun saveChatRoomMember(
        chatRoom: ChatRoom,
        memberEntity: MemberEntity,
    ) {
        chatRoomMemberJpaRepository.save(ChatRoomMember(chatRoom = chatRoom, memberEntity = memberEntity))
    }

    @Transactional(readOnly = true)
    fun findChatRoomsByMember(member: MemberEntity): List<ChatRoomMember> = chatRoomMemberJpaRepository.findByMemberEntity(member)

    @Transactional(readOnly = true)
    fun findPartner(
        chatRoom: ChatRoom,
        requester: MemberEntity,
    ): ChatRoomMember =
        chatRoomMemberJpaRepository.findByChatRoomAndMemberEntityNot(chatRoom, requester)
            ?: throw IllegalArgumentException("채팅방에 자신을 제외한 다른 사용자가 존재하지 않습니다.")

    @Transactional(readOnly = true)
    fun findChatRoomById(chatRoomId: Long): ChatRoom {
        val chatRoom = chatRoomJpaRepository.findByIdOrNull(chatRoomId)

        return chatRoom ?: throw IllegalArgumentException()
    }
}
