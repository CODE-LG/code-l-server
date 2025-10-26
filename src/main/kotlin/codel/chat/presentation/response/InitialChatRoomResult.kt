package codel.chat.presentation.response

data class InitialChatRoomResult(
    val approverChatRoomResponse: ChatRoomResponse,
    val senderChatRoomResponse: ChatRoomResponse
)
