package codel.chat.domain

import codel.chat.presentation.request.ChatRequest
import codel.member.domain.Member
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.springframework.data.annotation.CreatedDate
import java.time.LocalDateTime

@Entity
class Chat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_chat_room_member_id", nullable = false)
    var from: ChatRoomMember,
    @ManyToOne(optional = false)
    @JoinColumn(name = "to_chat_room_member_id", nullable = false)
    var to: ChatRoomMember,
    var message: String,
    var chatType: ChatType,
    @CreatedDate
    var sentAt: LocalDateTime? = null,
) {
    companion object {
        fun of(
            from: ChatRoomMember,
            to: ChatRoomMember,
            chatRequest: ChatRequest,
        ): Chat {
            if (from.chatRoom != to.chatRoom) {
                throw IllegalArgumentException("채팅의 채팅방 정보가 다릅니다.")
            }
            return Chat(
                id = null,
                from = from,
                to = to,
                message = chatRequest.message,
                chatType = chatRequest.chatType,
            )
        }
    }

    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("chatId가 존재하지 않습니다.")

    fun getChatType(requester: Member): ChatType {
        if (chatType == ChatType.RECOMMEND_TOPIC) return ChatType.RECOMMEND_TOPIC

        return when (requester) {
            from.member -> ChatType.MY
            to.member -> ChatType.PARTNER
            else -> throw IllegalArgumentException("채팅 타입이 잘못되었습니다.")
        }
    }

    fun getSentAtOrThrow(): LocalDateTime = sentAt ?: throw IllegalStateException("채팅 발송 시간이 설정되지 않았습니다.")
}
