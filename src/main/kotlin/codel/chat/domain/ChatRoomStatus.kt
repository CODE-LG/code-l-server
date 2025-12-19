package codel.chat.domain

enum class ChatRoomStatus(statusName: String) {
    DISABLED("폐지"),
    LOCKED("코드잠김"),
    UNLOCKED("코드해제"),
}
