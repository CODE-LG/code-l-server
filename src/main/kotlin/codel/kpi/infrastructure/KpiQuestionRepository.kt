package codel.kpi.infrastructure

import codel.chat.domain.ChatRoomQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

/**
 * KPI 집계 전용 질문 Repository
 *
 * 애플리케이션 로직과 분리하여 KPI 집계 성능 최적화 및 관심사 분리
 */
@Repository
interface KpiQuestionRepository : JpaRepository<ChatRoomQuestion, Long> {

    /**
     * 특정 UTC 기간 내 초기 질문이 아닌 질문을 사용한 채팅방 수
     * (질문하기 버튼 클릭으로 생성된 질문만 카운트)
     */
    @Query("""
        SELECT COUNT(DISTINCT crq.chatRoom.id)
        FROM ChatRoomQuestion crq
        WHERE crq.isInitial = false
        AND crq.createdAt >= :start
        AND crq.createdAt < :end
    """)
    fun countDistinctChatRoomsByCreatedAtBetweenExcludingInitial(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int

    /**
     * 특정 UTC 기간 내 질문하기 버튼 클릭 수
     * (초기 질문 제외, 모든 질문 카운트)
     */
    @Query("""
        SELECT COUNT(crq)
        FROM ChatRoomQuestion crq
        WHERE crq.isInitial = false
        AND crq.createdAt >= :start
        AND crq.createdAt < :end
    """)
    fun countQuestionClicksByCreatedAtBetweenExcludingInitial(
        @Param("start") start: LocalDateTime,
        @Param("end") end: LocalDateTime
    ): Int
}
