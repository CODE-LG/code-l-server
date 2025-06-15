package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.member.infrastructure.entity.MemberEntity

class ChatResponses(
    val chats: List<ChatResponse>,
) {
    companion object {
        fun of(
            chats: List<Chat>,
            requester: MemberEntity,
        ): ChatResponses = ChatResponses(chats.map { chat -> ChatResponse.of(chat, requester) })
    }
}
