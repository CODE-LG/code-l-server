package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.member.domain.Member

class ChatResponses(
    val chats: List<ChatResponse>,
) {
    companion object {
        fun of(
            chats: List<Chat>,
            requester: Member,
        ): ChatResponses = ChatResponses(chats = chats.map { chat -> ChatResponse.of(chat, requester) })
    }
}
