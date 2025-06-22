package codel.chat.presentation.response

import codel.chat.domain.Chat
import codel.member.domain.Member
import java.time.LocalDateTime

data class ChatResponses(
    val partner: PartnerResponse,
    val chats: List<ChatResponse>,
    val roomExpireTime: LocalDateTime,
    val roomType: String,
) {
    companion object {
        // TODO. 방 만료시간, 방 타입 추가
        fun of(
            requester: Member,
            partner: Member,
            chats: List<Chat>,
        ): ChatResponses =
            ChatResponses(
                partner = PartnerResponse.of(partner),
                chats = chats.map { chat -> ChatResponse.of(requester, chat) },
                roomExpireTime = LocalDateTime.now(),
                roomType = "",
            )
    }
}
