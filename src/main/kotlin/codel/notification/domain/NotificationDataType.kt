package codel.notification.domain

/**
 * FCM 알림 데이터 타입
 * 클라이언트에서 알림을 받았을 때 어떤 종류의 알림인지 구분하기 위한 타입
 */
enum class NotificationDataType(val value: String) {
    /**
     * 일반 채팅 메시지
     */
    CHAT("CHAT"),

    /**
     * 코드 해제 요청
     */
    CODE_UNLOCK_REQUEST("CODE_UNLOCK_REQUEST"),

    /**
     * 코드 해제 완료
     */
    CODE_UNLOCKED("CODE_UNLOCKED"),

    /**
     * 시그널(좋아요) 수신
     */
    SIGNAL("SIGNAL"),

    /**
     * 매칭 성공
     */
    MATCHING("MATCHING"),

    /**
     * 프로필 승인
     */
    PROFILE_APPROVED("PROFILE_APPROVED"),

    /**
     * 프로필 반려
     */
    PROFILE_REJECTED("PROFILE_REJECTED"),

    /**
     * 일일 매칭 알림 (데일리 코드)
     */
    DAILY_MATCHING("DAILY_MATCHING"),

    /**
     * 시스템 공지사항
     */
    NOTICE("NOTICE");

    override fun toString(): String = value
}
