package codel.chat.infrastructure

import codel.chat.domain.ChatRoomMember
import codel.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMember, Long> {
    fun findByMember(member: Member): List<ChatRoomMember>

    fun findByChatRoomIdAndMemberNot(
        chatRoomId: Long,
        excludeMember: Member,
    ): ChatRoomMember?

    fun findByChatRoomIdAndMember(
        chatRoomId: Long,
        member: Member,
    ): ChatRoomMember?
}
