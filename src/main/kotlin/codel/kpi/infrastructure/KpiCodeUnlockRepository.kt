package codel.kpi.infrastructure

import codel.chat.domain.CodeUnlockRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * KPI 집계 전용 코드해제 Repository
 *
 * 애플리케이션 로직과 분리하여 KPI 집계 성능 최적화 및 관심사 분리
 */
@Repository
interface KpiCodeUnlockRepository : JpaRepository<CodeUnlockRequest, Long> {

    /**
     * 특정 UTC 기간 생성된 코드해제 요청 개수
     */
    fun countByCreatedAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 승인된 코드해제 개수
     */
    @Query("""
        SELECT COUNT(cur) FROM CodeUnlockRequest cur
        WHERE cur.status = 'APPROVED'
        AND cur.updatedAt >= :start
        AND cur.updatedAt < :end
    """)
    fun countApprovedByUpdatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int
}
