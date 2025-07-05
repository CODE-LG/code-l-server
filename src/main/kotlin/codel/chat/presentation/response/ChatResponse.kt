package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.chat.domain.ChatType
import codel.member.domain.Member
import java.time.LocalDateTime

data class ChatResponse(
    val chatId: Long,
    val chatRoomId: Long,
    val message: String,
    val chatType: ChatType,
    val sentAt: LocalDateTime,
) {
    companion object {
        fun toResponse(
            requester: Member,
            chat: Chat,
        ): ChatResponse =
            ChatResponse(
                chatId = chat.getIdOrThrow(),
                chatRoomId = chat.chatRoom.getIdOrThrow(),
                chatType = chat.getChatType(requester),
                message = chat.message,
                sentAt = chat.getSentAtOrThrow(),
            )
    }
}
