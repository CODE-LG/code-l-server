package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.chat.domain.ChatType
import codel.member.domain.Member
import java.time.LocalDateTime

data class ChatResponse(
    val chatId: Long,
    val chatType: ChatType,
    val message: String,
    val sentAt: LocalDateTime,
) {
    companion object {
        fun of(
            chat: Chat,
            requester: Member,
        ): ChatResponse =
            ChatResponse(
                chatId = chat.getIdOrThrow(),
                chatType = chat.getChatType(requester),
                message = chat.message,
                sentAt = chat.getSentAtOrThrow(),
            )
    }
}
