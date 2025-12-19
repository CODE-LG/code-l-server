package codel.chat.presentation.request

import java.time.LocalDate

data class CreateChatRoomRequest(
    val partnerId: Long,
    val recentChatTime : LocalDate,
)
