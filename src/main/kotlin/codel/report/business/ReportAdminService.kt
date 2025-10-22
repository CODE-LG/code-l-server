package codel.report.business

import codel.member.domain.Member
import codel.member.infrastructure.MemberJpaRepository
import codel.report.domain.Report
import codel.report.domain.ReportStatus
import codel.report.exception.ReportException
import codel.report.infrastructure.ReportJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class ReportAdminService(
    private val reportJpaRepository: ReportJpaRepository,
    private val memberJpaRepository: MemberJpaRepository
) {

    /**
     * 신고 목록 조회 (필터링, 페이징)
     */
    fun getReportsWithFilter(
        keyword: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        pageable: Pageable
    ): Page<Report> {
        val reportStatus = status?.takeIf { it.isNotBlank() }?.let { ReportStatus.valueOf(it) }
        
        val startDateTime = startDate?.takeIf { it.isNotBlank() }?.let { 
            LocalDate.parse(it).atStartOfDay() 
        }
        
        val endDateTime = endDate?.takeIf { it.isNotBlank() }?.let { 
            LocalDate.parse(it).atTime(23, 59, 59) 
        }
        
        val searchKeyword = keyword?.takeIf { it.isNotBlank() }
        
        return reportJpaRepository.findReportsWithFilter(
            searchKeyword,
            reportStatus,
            startDateTime,
            endDateTime,
            pageable
        )
    }

    /**
     * 신고 상세 조회
     */
    fun getReportDetail(reportId: Long): Report {
        return reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
    }

    /**
     * 통계: 전체 신고 수
     */
    fun getTotalReportsCount(): Long {
        return reportJpaRepository.count()
    }

    /**
     * 통계: 상태별 신고 수
     */
    fun getReportCountByStatus(status: ReportStatus): Long {
        return reportJpaRepository.countByStatus(status)
    }

    /**
     * 통계: 오늘 신고 수
     */
    fun getTodayReportsCount(): Long {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.atTime(23, 59, 59)
        return reportJpaRepository.countByCreatedAtBetween(startOfDay, endOfDay)
    }

    /**
     * 통계: 이번 주 신고 수
     */
    fun getWeeklyReportsCount(): Long {
        val now = LocalDateTime.now()
        val weekAgo = now.minusWeeks(1)
        return reportJpaRepository.countByCreatedAtBetween(weekAgo, now)
    }

    /**
     * 통계: 이번 달 신고 수
     */
    fun getMonthlyReportsCount(): Long {
        val now = LocalDateTime.now()
        val monthAgo = now.minusMonths(1)
        return reportJpaRepository.countByCreatedAtBetween(monthAgo, now)
    }

    /**
     * 통계: 상태별 신고 수 (대시보드용)
     */
    fun getReportStatusStats(): Map<String, Long> {
        return mapOf(
            "pending" to reportJpaRepository.countByStatus(ReportStatus.PENDING),
            "inProgress" to reportJpaRepository.countByStatus(ReportStatus.IN_PROGRESS),
            "resolved" to reportJpaRepository.countByStatus(ReportStatus.RESOLVED),
            "dismissed" to reportJpaRepository.countByStatus(ReportStatus.DISMISSED),
            "duplicate" to reportJpaRepository.countByStatus(ReportStatus.DUPLICATE)
        )
    }

    /**
     * 피신고자의 총 신고 횟수 조회
     */
    fun getReportedMemberReportCount(memberId: Long): Long {
        val member = memberJpaRepository.findById(memberId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        return reportJpaRepository.countByReported(member)
    }

    /**
     * 피신고자의 신고 이력 조회
     */
    fun getReportedMemberReports(memberId: Long, pageable: Pageable): Page<Report> {
        val member = memberJpaRepository.findById(memberId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다.") }
        return reportJpaRepository.findByReportedOrderByCreatedAtDesc(member, pageable)
    }

    /**
     * 신고 많이 받은 사용자 TOP N
     */
    fun getMostReportedMembers(days: Long = 30, limit: Int = 10): List<Pair<Member, Long>> {
        val since = LocalDateTime.now().minusDays(days)
        val pageable = Pageable.ofSize(limit)
        
        val results = reportJpaRepository.findMostReportedMembers(since, pageable)
        
        return results.map { array ->
            val member = array[0] as Member
            val count = (array[1] as Long)
            Pair(member, count)
        }
    }

    /**
     * 신고 처리 상태 변경
     */
    @Transactional
    fun updateReportStatus(reportId: Long, status: ReportStatus, note: String? = null) {
        val report = reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
        
        report.updateStatus(status, note)
        reportJpaRepository.save(report)
    }

    /**
     * 검토 시작
     */
    @Transactional
    fun startProcessing(reportId: Long) {
        val report = reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
        
        report.startProcessing()
        reportJpaRepository.save(report)
    }

    /**
     * 처리 완료
     */
    @Transactional
    fun resolveReport(reportId: Long, note: String? = null) {
        val report = reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
        
        report.resolve(note)
        reportJpaRepository.save(report)
    }

    /**
     * 반려
     */
    @Transactional
    fun dismissReport(reportId: Long, note: String? = null) {
        val report = reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
        
        report.dismiss(note)
        reportJpaRepository.save(report)
    }

    /**
     * 중복 신고로 처리
     */
    @Transactional
    fun markAsDuplicate(reportId: Long, note: String? = null) {
        val report = reportJpaRepository.findById(reportId)
            .orElseThrow { ReportException(HttpStatus.NOT_FOUND, "신고 내역을 찾을 수 없습니다.") }
        
        report.markAsDuplicate(note)
        reportJpaRepository.save(report)
    }
}
