package codel.signal.domain
 
enum class SignalStatus(val statusName: String) {
    PENDING("대기중"),
    ACCEPTED("수락됨"),
    REJECTED("거절됨")
} 