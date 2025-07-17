package codel.signal.domain
 
enum class SignalStatus(val statusName: String) {
    PENDING("매칭대기"),
    PENDING_HIDDEN("매칭대기_숨김"),
    APPROVED("매칭성공"),
    APPROVED_HIDDEN("매칭성공_숨김"),
    REJECTED("매칭거절");


    fun changeBlockedMessage(): String? = when (this) {
        REJECTED -> "이미 시그널 거절된 상대입니다."
        APPROVED, APPROVED_HIDDEN -> "이미 시그널 승인된 상대입니다."
        else -> null
    }
} 