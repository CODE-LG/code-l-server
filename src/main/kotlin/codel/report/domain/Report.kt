package codel.report.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.*

@Entity
class Report(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id : Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    val reporter: Member,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_id")
    val reported: Member,

    @Column(columnDefinition = "TEXT")
    val reason : String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: ReportStatus = ReportStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var adminNote: String? = null,  // 관리자 메모

    var processedAt: java.time.LocalDateTime? = null  // 처리 일시
) : BaseTimeEntity() {
    
    /**
     * 신고 처리 상태 변경
     */
    fun updateStatus(newStatus: ReportStatus, note: String? = null) {
        this.status = newStatus
        this.adminNote = note
        if (newStatus != ReportStatus.PENDING && newStatus != ReportStatus.IN_PROGRESS) {
            this.processedAt = java.time.LocalDateTime.now()
        }
    }

    /**
     * 처리 중으로 변경
     */
    fun startProcessing() {
        this.status = ReportStatus.IN_PROGRESS
    }

    /**
     * 처리 완료
     */
    fun resolve(note: String? = null) {
        updateStatus(ReportStatus.RESOLVED, note)
    }

    /**
     * 반려
     */
    fun dismiss(note: String? = null) {
        updateStatus(ReportStatus.DISMISSED, note)
    }

    /**
     * 중복으로 처리
     */
    fun markAsDuplicate(note: String? = null) {
        updateStatus(ReportStatus.DUPLICATE, note)
    }
}