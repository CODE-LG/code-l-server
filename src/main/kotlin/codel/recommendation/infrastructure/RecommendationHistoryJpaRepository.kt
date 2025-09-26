package codel.recommendation.infrastructure

import codel.member.domain.Member
import codel.recommendation.domain.RecommendationHistory
import codel.recommendation.domain.RecommendationType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * 추천 이력 관리 Repository
 * 중복 방지 및 추천 결과 추적을 위한 데이터 접근 계층
 */
interface RecommendationHistoryJpaRepository : JpaRepository<RecommendationHistory, Long> {
    
    /**
     * 특정 사용자가 최근 N일 내에 추천받은 사용자 ID 목록 조회
     * 중복 방지를 위해 사용
     */
    @Query("""
        SELECT rh.recommendedUser.id 
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendedAt >= :fromDateTime
    """)
    fun findRecommendedUserIdsInPeriod(
        @Param("user") user: Member,
        @Param("fromDateTime") fromDateTime: LocalDateTime
    ): List<Long>
    
    /**
     * 특정 추천 타입에서 최근 N일 내에 추천받은 사용자 ID 목록 조회
     * 타입별 중복 방지 정책에 사용
     */
    @Query("""
        SELECT rh.recommendedUser.id 
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendationType = :type 
        AND rh.recommendedAt >= :fromDateTime
    """)
    fun findRecommendedUserIdsByTypeInPeriod(
        @Param("user") user: Member,
        @Param("type") type: RecommendationType,
        @Param("fromDateTime") fromDateTime: LocalDateTime
    ): List<Long>
    
    /**
     * 오늘의 코드매칭에서 추천받은 사용자 ID (오늘 기준)
     * 24시간 유지를 위한 기존 추천 결과 조회
     */
    @Query("""
        SELECT rh.recommendedUser.id 
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendationType = 'DAILY_CODE_MATCHING'
        AND DATE(rh.recommendedAt) = :today
        ORDER BY rh.createdAt ASC
    """)
    fun findTodayDailyCodeMatchingIds(
        @Param("user") user: Member,
        @Param("today") today: LocalDate
    ): List<Long>
    
    /**
     * 특정 시간대 코드타임에서 추천받은 사용자 ID (오늘 기준)
     * 시간대별 추천 결과 조회
     */
    @Query("""
        SELECT rh.recommendedUser.id 
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendationType = 'CODE_TIME'
        AND rh.recommendationTimeSlot = :timeSlot
        AND DATE(rh.recommendedAt) = :today
        ORDER BY rh.createdAt ASC
    """)
    fun findTodayCodeTimeIdsBySlot(
        @Param("user") user: Member,
        @Param("timeSlot") timeSlot: String,
        @Param("today") today: LocalDate
    ): List<Long>

    /**
     * 특정 날짜의 추천 이력 존재 여부 확인
     * 추천 생성 여부 판단에 사용
     */
    @Query("""
        SELECT COUNT(rh) > 0
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendationType = :type
        AND DATE(rh.recommendedAt) = :date
    """)
    fun existsByUserAndTypeAndDate(
        @Param("user") user: Member,
        @Param("type") type: RecommendationType,
        @Param("date") date: LocalDate
    ): Boolean

    /**
     * 특정 시간대의 추천 이력 존재 여부 확인 (코드타임용)
     */
    @Query("""
        SELECT COUNT(rh) > 0
        FROM RecommendationHistory rh 
        WHERE rh.user = :user 
        AND rh.recommendationType = 'CODE_TIME'
        AND rh.recommendationTimeSlot = :timeSlot
        AND DATE(rh.recommendedAt) = :date
    """)
    fun existsByUserAndTimeSlotAndDate(
        @Param("user") user: Member,
        @Param("timeSlot") timeSlot: String,
        @Param("date") date: LocalDate
    ): Boolean

    /**
     * 오래된 추천 이력 삭제 (성능 최적화용)
     * 배치 작업에서 사용하여 테이블 크기 관리
     */
    fun deleteByRecommendedAtBefore(cutoffDateTime: LocalDateTime): Long

    /**
     * 사용자별 추천 이력 삭제 (회원 탈퇴 시)
     */
    fun deleteByUser(user: Member): Long
    
    /**
     * 추천받은 사용자 기준 이력 삭제 (회원 탈퇴 시)
     */
    fun deleteByRecommendedUser(recommendedUser: Member): Long

    /**
     * 통계를 위한 날짜별 추천 수 조회
     */
    @Query("""
        SELECT DATE(rh.recommendedAt), COUNT(rh)
        FROM RecommendationHistory rh 
        WHERE rh.recommendedAt >= :fromDateTime
        GROUP BY DATE(rh.recommendedAt)
        ORDER BY DATE(rh.recommendedAt) DESC
    """)
    fun countRecommendationsByDate(@Param("fromDateTime") fromDateTime: LocalDateTime): List<Array<Any>>

    /**
     * 사용자별 총 추천 받은 횟수
     */
    @Query("""
        SELECT COUNT(DISTINCT rh.recommendedUser.id)
        FROM RecommendationHistory rh 
        WHERE rh.user = :user
    """)
    fun countUniqueRecommendedUsers(@Param("user") user: Member): Long

    /**
     * 특정 기간 내 가장 많이 추천받은 사용자들 (인기도 측정)
     */
    @Query("""
        SELECT rh.recommendedUser.id, COUNT(rh) as recommendCount
        FROM RecommendationHistory rh 
        WHERE rh.recommendedAt >= :fromDateTime
        GROUP BY rh.recommendedUser.id
        ORDER BY recommendCount DESC
    """)
    fun findMostRecommendedUsers(
        @Param("fromDateTime") fromDateTime: LocalDateTime,
        pageable: org.springframework.data.domain.Pageable
    ): List<Array<Any>>
}
