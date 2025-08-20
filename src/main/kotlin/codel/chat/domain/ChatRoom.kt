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
    @JoinColumn(name = "recent_chat_id")
    var recentChat: Chat? = null,

    @Enumerated(EnumType.STRING)
    var status: ChatRoomStatus = ChatRoomStatus.LOCKED,

    var isUnlocked: Boolean = false,

    var unlockedAt: LocalDateTime? = null,
) : BaseTimeEntity() {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")

    fun updateRecentChat(recentChat: Chat) {
        this.recentChat = recentChat
    }

    fun getUnlockedUpdateAtOrThrow() = unlockedAt ?: throw ChatException(HttpStatus.BAD_REQUEST, "코드 해제 요청 또는 승인한 적이 없습니다.")

    fun unlock(){
        isUnlocked = true
        status = ChatRoomStatus.UNLOCKED
        unlockedAt = LocalDateTime.now()
    }

    fun reject(){
        status = ChatRoomStatus.LOCKED
    }

    fun closeConversation() {
        status = ChatRoomStatus.DISABLED
    }
}
