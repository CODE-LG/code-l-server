package codel.report.domain

/**
 * 신고 처리 상태
 */
enum class ReportStatus(val description: String) {
    PENDING("미처리"),        // 신고 접수됨, 아직 검토 안함
    IN_PROGRESS("검토중"),    // 관리자가 검토 중
    RESOLVED("처리완료"),     // 조치 완료 (경고/정지 등)
    DISMISSED("반려"),        // 부적절한 신고로 반려
    DUPLICATE("중복신고")     // 이미 처리된 동일 신고
}
