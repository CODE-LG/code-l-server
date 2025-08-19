package codel.chat.domain

enum class ChatContentType {
    TEXT,
    MATCHED,
    UNLOCKED,
    UNLOCKED_REQUEST,
    UNLOCKED_APPROVED,      // 2단계 추가
    UNLOCKED_REJECTED,      // 2단계 추가
    QUESTION,
    ONBOARDING,
    TIME,
    DISABLED,
    MEMBER_LEFT,
}
