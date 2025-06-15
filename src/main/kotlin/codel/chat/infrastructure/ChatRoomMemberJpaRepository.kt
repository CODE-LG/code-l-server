package codel.chat.infrastructure

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMember, Long> {
    fun findByMemberEntity(member: MemberEntity): List<ChatRoomMember>

    fun findByChatRoomAndMemberEntityNot(
        chatRoom: ChatRoom,
        excludeMember: MemberEntity,
    ): ChatRoomMember?

    fun findByChatRoomAndMemberEntity(
        chatRoom: ChatRoom,
        excludeMember: MemberEntity,
    ): ChatRoomMember?
}
