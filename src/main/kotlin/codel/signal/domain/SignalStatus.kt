package codel.signal.domain
 
enum class SignalStatus(val statusName: String) {
    PENDING("대기중"),

    PENDING_HIDDEN("대기_숨김"),
    ACCEPTED("수락됨"),
    ACCEPTED_HIDDEN("승인_숨김"),
    REJECTED("거절됨");


    fun changeBlockedMessage(): String? = when (this) {
        REJECTED -> "이미 시그널 거절된 상대입니다."
        ACCEPTED, ACCEPTED_HIDDEN -> "이미 시그널 승인된 상대입니다."
        else -> null
    }
} 