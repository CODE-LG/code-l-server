package codel.report.infrastructure

import codel.member.domain.Member
import codel.report.domain.Report
import codel.report.domain.ReportStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ReportJpaRepository : JpaRepository<Report, Long> {
    
    /**
     * 상태별 신고 개수 조회
     */
    fun countByStatus(status: ReportStatus): Long
    
    /**
     * 특정 기간 내 신고 개수 조회
     */
    fun countByCreatedAtBetween(startDate: LocalDateTime, endDate: LocalDateTime): Long
    
    /**
     * 피신고자별 신고 횟수 조회
     */
    fun countByReported(reported: Member): Long
    
    /**
     * 피신고자의 신고 이력 조회 (FETCH JOIN 추가)
     */
    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter
        JOIN FETCH r.reported
        WHERE r.reported = :reported
        ORDER BY r.createdAt DESC
    """)
    fun findByReportedOrderByCreatedAtDesc(@Param("reported") reported: Member, pageable: Pageable): Page<Report>

    /**
     * 상태별 신고 목록 조회 (FETCH JOIN 추가)
     */
    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter
        JOIN FETCH r.reported
        WHERE r.status = :status
        ORDER BY r.createdAt DESC
    """)
    fun findByStatusOrderByCreatedAtDesc(@Param("status") status: ReportStatus, pageable: Pageable): Page<Report>

    /**
     * 전체 신고 목록 조회 (FETCH JOIN 추가)
     */
    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter
        JOIN FETCH r.reported
        ORDER BY r.createdAt DESC
    """)
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Report>

    /**
     * ID로 신고 조회 (FETCH JOIN 추가)
     */
    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter reporter
        JOIN FETCH r.reported reported
        LEFT JOIN FETCH reporter.profile
        LEFT JOIN FETCH reported.profile
        WHERE r.id = :id
    """)
    fun findByIdWithMembers(@Param("id") id: Long): Report?
    
    /**
     * 복합 필터링 쿼리
     * - 키워드: 신고 사유, 신고자 이름, 피신고자 이름
     * - 상태 필터
     * - 날짜 범위
     */
    @Query("""
        SELECT r FROM Report r
        JOIN FETCH r.reporter reporter
        JOIN FETCH r.reported reported
        LEFT JOIN reporter.profile reporterProfile
        LEFT JOIN reported.profile reportedProfile
        WHERE (:keyword IS NULL 
            OR r.reason LIKE %:keyword%
            OR reporter.email LIKE %:keyword%
            OR reported.email LIKE %:keyword%
            OR reporterProfile.codeName LIKE %:keyword%
            OR reportedProfile.codeName LIKE %:keyword%)
        AND (:status IS NULL OR r.status = :status)
        AND (:startDate IS NULL OR r.createdAt >= :startDate)
        AND (:endDate IS NULL OR r.createdAt <= :endDate)
        ORDER BY r.createdAt DESC
    """)
    fun findReportsWithFilter(
        @Param("keyword") keyword: String?,
        @Param("status") status: ReportStatus?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<Report>
    
    /**
     * 신고 많이 받은 사용자 TOP N
     */
    @Query("""
        SELECT r.reported, COUNT(r) as reportCount
        FROM Report r
        WHERE r.createdAt >= :since
        GROUP BY r.reported
        ORDER BY reportCount DESC
    """)
    fun findMostReportedMembers(
        @Param("since") since: LocalDateTime,
        pageable: Pageable
    ): List<Array<Any>>
}