package codel.chat.infrastructure

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMember, Long> {
    fun findByMember(member: Member): List<ChatRoomMember>

    fun findByChatRoomAndMemberNot(
        chatRoom: ChatRoom,
        excludeMember: Member,
    ): ChatRoomMember?

    fun findByChatRoomAndMember(
        chatRoom: ChatRoom,
        excludeMember: Member,
    ): ChatRoomMember?
}
