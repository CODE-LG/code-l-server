package codel.chat.infrastructure

import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatRoomMemberStatus
import codel.chat.domain.ChatRoomStatus
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomMemberJpaRepository : JpaRepository<ChatRoomMember, Long> {
    fun findAllByMember(member: Member): List<ChatRoomMember>

    fun findByChatRoomIdAndMemberNot(
        chatRoomId: Long,
        excludeMember: Member,
    ): ChatRoomMember?

    fun findByChatRoomIdAndMember(
        chatRoomId: Long,
        member: Member,
    ): ChatRoomMember?

    @Query(
        """
        SELECT crmOther
        FROM ChatRoomMember crmMe
        JOIN crmMe.chatRoom cr
        JOIN ChatRoomMember crmOther ON crmOther.chatRoom = cr
        JOIN FETCH crmOther.member m
        JOIN FETCH m.profile p
        WHERE crmMe.member = :me
        AND cr.status = :status
        AND crmOther.member != :me
    """
    )
    fun findUnlockedOpponentsWithProfile(
        @Param("me") me: Member,
        @Param("status") status: ChatRoomStatus,
        pageable: Pageable
    ): Page<ChatRoomMember>

    @Query("SELECT crm FROM ChatRoomMember crm WHERE crm.chatRoom.id = :chatRoomId")
    fun findByChatRoomId(@Param("chatRoomId") chatRoomId: Long): List<ChatRoomMember>

    /**
     * 새로 추가된 메서드들
     */
    fun findByMemberAndMemberStatus(
        member: Member,
        memberStatus: ChatRoomMemberStatus,
        pageable: Pageable
    ): Page<ChatRoomMember>
}
