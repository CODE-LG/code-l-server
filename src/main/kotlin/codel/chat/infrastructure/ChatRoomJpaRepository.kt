package codel.chat.infrastructure

import codel.chat.domain.ChatRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomJpaRepository : JpaRepository<ChatRoom, Long> {
    @Query(
        """
            SELECT cr 
            FROM ChatRoom cr
             JOIN ChatRoomMember crm 
            ON crm.chatRoom.id = cr.id
            WHERE crm.member.id = :memberId
        """,
    )
    fun findMyChatRoomWithPageable(
        memberId: Long,
        pageable: Pageable,
    ): Page<ChatRoom>
}
