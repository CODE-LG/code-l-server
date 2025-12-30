package codel.kpi.infrastructure

import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * KPI 집계 전용 채팅방 Repository
 *
 * 애플리케이션 로직과 분리하여 KPI 집계 성능 최적화 및 관심사 분리
 */
@Repository
interface KpiChatRepository : JpaRepository<ChatRoom, Long> {

    /**
     * 특정 UTC 기간 생성된 채팅방 개수
     */
    fun countByCreatedAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 생성된 채팅방 ID 목록 조회
     */
    @Query("SELECT cr.id FROM ChatRoom cr WHERE cr.createdAt >= :start AND cr.createdAt < :end")
    fun findIdsByCreatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<Long>

    /**
     * 특정 UTC 기간 생성된 채팅방 조회
     */
    fun findByCreatedAtBetween(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ChatRoom>

    /**
     * 특정 UTC 기간 생성되고 템플릿 이후 실제 메시지가 있는 채팅방 수
     * (첫 메시지 전송률 계산용)
     */
    @Query(value = """
        SELECT COUNT(DISTINCT cr.id)
        FROM chat_room cr
        WHERE cr.created_at >= :start
          AND cr.created_at < :end
          AND (
            SELECT COUNT(*)
            FROM chat c
            WHERE c.chat_room_id = cr.id
          ) > 6
    """, nativeQuery = true)
    fun countChatRoomsWithFirstMessage(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 시점 기준 최근 7일 내 활동이 있는 활성 채팅방 수
     */
    @Query(value = """
        SELECT COUNT(DISTINCT cr.id)
        FROM chat_room cr
        WHERE cr.status != 'DISABLED'
          AND cr.created_at < :asOfUtc
          AND EXISTS (
            SELECT 1
            FROM chat c
            WHERE c.chat_room_id = cr.id
              AND c.sent_at >= :sevenDaysAgoUtc
              AND c.sent_at < :asOfUtc
          )
    """, nativeQuery = true)
    fun countActiveChatroomsAsOfDate(
        @Param("asOfUtc") asOfUtc: LocalDateTime,
        @Param("sevenDaysAgoUtc") sevenDaysAgoUtc: LocalDateTime
    ): Int

    /**
     * 특정 UTC 시점 기준 열린 채팅방 수 (종료되지 않은 모든 채팅방)
     */
    @Query("""
        SELECT COUNT(cr) FROM ChatRoom cr
        WHERE cr.status != 'DISABLED'
        AND cr.createdAt < :asOfUtc
    """)
    fun countOpenChatroomsAsOfDate(
        @Param("asOfUtc") asOfUtc: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 종료된 채팅방 조회
     */
    @Query("""
        SELECT cr FROM ChatRoom cr
        WHERE cr.status = 'DISABLED'
        AND cr.updatedAt >= :start
        AND cr.updatedAt < :end
    """)
    fun findClosedByUpdatedAtBetween(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): List<ChatRoom>
}
