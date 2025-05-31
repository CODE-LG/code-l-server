package codel.chat.infrastructure

import codel.chat.infrastructure.entity.ChatRoomMemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMemberEntity, Long> {
    fun findByChatRoomEntityIdAndMemberEntityIdNot(
        chatRoomId: Long,
        excludeMemberId: Long,
    ): ChatRoomMemberEntity?
}
