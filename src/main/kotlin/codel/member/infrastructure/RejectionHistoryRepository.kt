package codel.member.infrastructure

import codel.member.domain.RejectionHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface RejectionHistoryRepository : JpaRepository<RejectionHistory, Long> {
    
    /**
     * 특정 회원의 모든 거절 이력 조회 (최신순)
     */
    fun findByMemberIdOrderByRejectedAtDesc(memberId: Long): List<RejectionHistory>
    
    /**
     * 특정 회원의 특정 차수 거절 이력 조회
     */
    fun findByMemberIdAndRejectionRound(memberId: Long, rejectionRound: Int): List<RejectionHistory>
    
    /**
     * 특정 회원의 최대 거절 차수 조회
     */
    @Query("SELECT COALESCE(MAX(rh.rejectionRound), 0) FROM RejectionHistory rh WHERE rh.member.id = :memberId")
    fun findMaxRejectionRoundByMemberId(memberId: Long): Int
    
    /**
     * 특정 회원의 거절 이력 개수 조회
     */
    fun countByMemberId(memberId: Long): Long
}
