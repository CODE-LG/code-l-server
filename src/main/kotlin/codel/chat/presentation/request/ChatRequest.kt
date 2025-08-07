package codel.chat.presentation.request

import codel.chat.domain.ChatSenderType
import java.time.LocalDateTime

data class ChatRequest(
    val message: String,
    val memberId: Long,
    val chatType: ChatSenderType,
    val recentChatTime: LocalDateTime,
)
