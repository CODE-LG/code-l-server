package codel.chat.domain

enum class UnlockRequestStatus(val description: String) {
    PENDING("대기중"),
    APPROVED("승인됨"),    // 2단계에서 사용
    REJECTED("거절됨"),    // 2단계에서 사용  
}
