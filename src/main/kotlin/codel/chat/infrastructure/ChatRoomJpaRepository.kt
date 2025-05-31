package codel.chat.infrastructure

import codel.chat.infrastructure.entity.ChatRoomEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomJpaRepository : JpaRepository<ChatRoomEntity, Long> {
    @Query(
        "SELECT cr FROM ChatRoomEntity cr JOIN ChatRoomMemberEntity crm ON crm.chatRoomEntity.id = cr.id WHERE crm.memberEntity.id = :memberId",
    )
    fun findByMemberIdIn(memberId: Long): List<ChatRoomEntity>
}
