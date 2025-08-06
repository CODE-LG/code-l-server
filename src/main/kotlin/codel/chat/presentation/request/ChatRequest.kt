package codel.chat.presentation.request

import codel.chat.domain.ChatSenderType

data class ChatRequest(
    val message: String,
    val memberId: Long,
    val chatType: ChatSenderType,
)
