package codel.chat.domain

import codel.chat.exception.ChatException
import codel.common.domain.BaseTimeEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Entity
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var isActive: Boolean = true,
    @OneToOne
    @JoinColumn(name = "chat_id")
    var recentChat: Chat? = null,

    var status : ChatRoomStatus = ChatRoomStatus.LOCKED,

    var unlockedRequestedBy : Long? = null,

    var unlockedUpdateAt : LocalDateTime? = null,
) : BaseTimeEntity() {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")

    fun unlock(memberId : Long) {
        when (status) {
            ChatRoomStatus.LOCKED -> {
                status = ChatRoomStatus.LOCKED_REQUESTED
                unlockedUpdateAt = LocalDateTime.now()
                unlockedRequestedBy = memberId
            }
            ChatRoomStatus.LOCKED_REQUESTED -> {
                if (unlockedRequestedBy == memberId) {
                    throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드해제 요청을 보낸 상태입니다.")
                }
                status = ChatRoomStatus.UNLOCKED
                unlockedUpdateAt = LocalDateTime.now()
            }
            ChatRoomStatus.UNLOCKED -> {
                throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드해제된 방입니다.")
            }
        }
    }
}
