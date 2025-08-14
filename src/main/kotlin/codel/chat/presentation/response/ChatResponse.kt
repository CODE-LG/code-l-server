package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatSenderType
import codel.member.domain.Member
import java.time.LocalDateTime

data class ChatResponse(
    val chatId: Long,
    val chatRoomId: Long,
    val message: String,
    val chatType: ChatSenderType,
    val senderId : Long,
    val contentType: ChatContentType,
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
                senderId = chat.getFromChatRoomMemberOrThrow().member.getIdOrThrow(),
                contentType = chat.chatContentType,
                sentAt = chat.getSentAtOrThrow(),
            )
    }
}
