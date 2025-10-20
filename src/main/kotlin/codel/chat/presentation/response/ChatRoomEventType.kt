package codel.chat.presentation.response

enum class ChatRoomEventType {
    UPDATE,    // 메시지 송수신, 읽음 처리 등 일반 업데이트
    REMOVED    // 사용자가 채팅방을 나감
}
