package codel.chat.domain

import codel.chat.exception.ChatException
import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Entity
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @OneToOne
    @JoinColumn(name = "chat_id")
    var recentChat: Chat? = null,

    @Enumerated(EnumType.STRING)
    var status: ChatRoomStatus = ChatRoomStatus.LOCKED,

    var unlockedRequestedBy: Long? = null,

    var unlockedUpdateAt: LocalDateTime? = null,
) : BaseTimeEntity() {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")

    fun unlock(memberId: Long) {
        when (status) {
            ChatRoomStatus.LOCKED -> {
                status = ChatRoomStatus.UNLOCKED_REQUESTED
                unlockedUpdateAt = LocalDateTime.now()
                unlockedRequestedBy = memberId
            }

            ChatRoomStatus.UNLOCKED_REQUESTED -> {
                if (unlockedRequestedBy == memberId) {
                    throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드해제 요청을 보낸 상태입니다.")
                }
                status = ChatRoomStatus.UNLOCKED
                unlockedUpdateAt = LocalDateTime.now()
            }

            ChatRoomStatus.UNLOCKED -> {
                throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드해제된 방입니다.")
            }

            ChatRoomStatus.DISABLED -> {
                throw ChatException(HttpStatus.BAD_REQUEST, "폐지된 채팅방입니다.")
            }
        }
    }

    fun updateRecentChat(recentChat: Chat) {
        this.recentChat = recentChat
    }


    fun getUnlockedUpdateAtOrThrow() = unlockedUpdateAt ?: throw ChatException(HttpStatus.BAD_REQUEST, "코드 해제 요청 또는 승인한 적이 없습니다.")
}
