package codel.chat.domain

enum class ChatRoomStatus(statusName : String) {
    LOCKED("코드잠김"),
    LOCKED_REQUESTED("코드해제_요청"),
    UNLOCKED("코드해제"),
}
