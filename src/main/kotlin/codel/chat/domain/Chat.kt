package codel.chat.domain

import codel.chat.exception.ChatException
import codel.chat.presentation.request.ChatSendRequest
import codel.member.domain.Member
import jakarta.persistence.*
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

    @ManyToOne
    @JoinColumn(name = "from_chat_room_member_id", nullable = true)
    var fromChatRoomMember: ChatRoomMember?,
    var message: String,

    @Enumerated(EnumType.STRING)
    var senderType : ChatSenderType,

    @Enumerated(EnumType.STRING)
    var chatContentType : ChatContentType,

    @CreatedDate
    var sentAt: LocalDateTime? = null,
) {
    companion object {
        fun of(
            fromChatRoomMember: ChatRoomMember,
            chatSendRequest: ChatSendRequest,
        ): Chat =
            Chat(
                id = null,
                chatRoom = fromChatRoomMember.chatRoom,
                fromChatRoomMember = fromChatRoomMember,
                message = chatSendRequest.message,
                senderType = chatSendRequest.chatType,
                chatContentType = ChatContentType.TEXT
            )

        fun createSystemMessage(
            chatRoom : ChatRoom,
            message : String,
            chatContentType: ChatContentType,
        ): Chat =
            Chat(
                id = null,
                chatRoom = chatRoom,
                message = message,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = chatContentType,
                fromChatRoomMember = null,
                sentAt = LocalDateTime.now(),
            )
    }

    fun getIdOrThrow(): Long = id ?: throw ChatException(HttpStatus.BAD_REQUEST, "chatId가 존재하지 않습니다.")

    fun getSentAtOrThrow(): LocalDateTime =
        sentAt ?: throw ChatException(HttpStatus.BAD_REQUEST, "채팅 발송 시간이 설정되지 않았습니다.")

    fun getChatType(requester: Member): ChatSenderType {
        return when {
            senderType == ChatSenderType.SYSTEM -> ChatSenderType.SYSTEM
            requester == getFromChatRoomMemberOrThrow() -> ChatSenderType.MY
            else -> ChatSenderType.PARTNER
        }
    }

    fun getFromChatRoomMemberOrThrow(): ChatRoomMember = fromChatRoomMember ?: throw ChatException(
        HttpStatus.BAD_REQUEST, "채팅과 관련된 회원을 찾을 수 없습니다.")
}
