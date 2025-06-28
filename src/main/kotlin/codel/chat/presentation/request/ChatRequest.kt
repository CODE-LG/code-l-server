package codel.chat.presentation.request

import codel.chat.domain.ChatType

data class ChatRequest(
    val message: String,
    val memberId: Long,
    val chatType: ChatType,
)
