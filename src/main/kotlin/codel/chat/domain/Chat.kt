package codel.chat.domain

import codel.chat.exception.ChatException
import codel.chat.presentation.request.ChatRequest
import codel.member.domain.Member
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.springframework.data.annotation.CreatedDate
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Entity
class Chat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,
    @ManyToOne(optional = false)
    @JoinColumn(name = "from_chat_room_member_id", nullable = false)
    var from: ChatRoomMember,
    var message: String,
    @Enumerated(EnumType.STRING)
    var chatType: ChatType,
    @CreatedDate
    var sentAt: LocalDateTime? = null,
) {
    companion object {
        fun of(
            from: ChatRoomMember,
            chatRequest: ChatRequest,
        ): Chat =
            Chat(
                id = null,
                chatRoom = from.chatRoom,
                from = from,
                message = chatRequest.message,
                chatType = chatRequest.chatType,
            )
    }

    fun getIdOrThrow(): Long = id ?: throw ChatException(HttpStatus.BAD_REQUEST, "chatId가 존재하지 않습니다.")

    fun getSentAtOrThrow(): LocalDateTime = sentAt ?: throw ChatException(HttpStatus.BAD_REQUEST, "채팅 발송 시간이 설정되지 않았습니다.")

    fun getChatType(requester: Member): ChatType =
        when (requester) {
            from.member -> ChatType.MY
            else -> ChatType.PARTNER
        }
}
