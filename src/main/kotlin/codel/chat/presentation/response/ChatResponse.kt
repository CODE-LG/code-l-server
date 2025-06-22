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
    val isRead: Boolean,
) {
    companion object {
        // TODO. 읽은 채팅인지 구분
        fun of(
            requester: Member,
            chat: Chat,
        ): ChatResponse =
            ChatResponse(
                chatId = chat.getIdOrThrow(),
                chatType = chat.getChatType(requester),
                message = chat.message,
                sentAt = chat.getSentAtOrThrow(),
                isRead = false,
            )
    }
}
