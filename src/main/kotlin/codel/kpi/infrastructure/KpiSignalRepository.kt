package codel.kpi.infrastructure

import codel.signal.domain.Signal
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * KPI 집계 전용 시그널 Repository
 *
 * 애플리케이션 로직과 분리하여 KPI 집계 성능 최적화 및 관심사 분리
 */
@Repository
interface KpiSignalRepository : JpaRepository<Signal, Long> {

    /**
     * 특정 UTC 기간 생성된 시그널 개수
     */
    fun countByCreatedAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 승인된 시그널 개수 (updatedAt 기준)
     */
    @Query("""
        SELECT COUNT(s) FROM Signal s
        WHERE s.senderStatus = 'APPROVED'
        AND s.updatedAt >= :start
        AND s.updatedAt < :end
    """)
    fun countApprovedByUpdatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 생성된 시그널 중 승인된 개수 (createdAt 기준)
     * 시그널 수락률 계산용: 특정 날짜에 보낸 시그널 중 현재까지 승인된 개수
     */
    @Query("""
        SELECT COUNT(s) FROM Signal s
        WHERE s.createdAt >= :start
        AND s.createdAt < :end
        AND s.senderStatus = 'APPROVED'
    """)
    fun countApprovedByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int
}
