package codel.chat.presentation.response

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * WebSocket 채팅 에러 응답 DTO
 */
data class ChatErrorResponse(
    val timestamp: String = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    val errorType: ChatErrorType,
    val message: String,
    val chatRoomId: Long? = null
)

enum class ChatErrorType {
    VALIDATION_ERROR,      // 유효성 검증 실패 (메시지 전송 불가 등)
    PERMISSION_DENIED,     // 권한 없음
    SAVE_FAILED,           // 저장 실패
    INTERNAL_ERROR         // 서버 내부 오류
}
