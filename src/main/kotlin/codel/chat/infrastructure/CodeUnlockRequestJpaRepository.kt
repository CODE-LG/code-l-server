package codel.chat.infrastructure

import codel.chat.domain.CodeUnlockRequest
import codel.chat.domain.UnlockRequestStatus
import codel.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CodeUnlockRequestJpaRepository : JpaRepository<CodeUnlockRequest, Long> {

    @Query("""
        SELECT cur FROM CodeUnlockRequest cur 
        WHERE cur.chatRoom.id = :chatRoomId 
        AND cur.status = :status
        ORDER BY cur.requestedAt DESC
    """)
    fun findByChatRoomIdAndStatus(chatRoomId: Long, status: UnlockRequestStatus): List<CodeUnlockRequest>

    @Query("""
        SELECT cur FROM CodeUnlockRequest cur 
        WHERE cur.chatRoom.id = :chatRoomId 
        AND cur.requester = :requester 
        AND cur.status = 'PENDING'
    """)
    fun findPendingRequestByRequester(chatRoomId: Long, requester: Member): CodeUnlockRequest?

    @Query("""
        SELECT cur FROM CodeUnlockRequest cur 
        WHERE cur.chatRoom.id = :chatRoomId 
        ORDER BY cur.requestedAt DESC
        LIMIT 1
    """)
    fun findLatestByChatRoomId(chatRoomId: Long): CodeUnlockRequest?

    /**
     * ID로 코드해제 요청 조회 (2단계에서 사용)
     */
    fun findByIdAndStatus(requestId: Long, status: UnlockRequestStatus): CodeUnlockRequest?
}
