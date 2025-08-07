package codel.chat.presentation.request

import codel.chat.domain.ChatSenderType
import java.time.LocalDateTime

data class ChatSendRequest(
    val message: String,
    val memberId: Long,
    val chatType: ChatSenderType,
    val recentChatTime: LocalDateTime,
)
