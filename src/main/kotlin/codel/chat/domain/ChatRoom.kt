package codel.chat.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var isActive: Boolean = true,
) {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")
}

data class ChatRoomInfo(
    val partner: ChatRoomMember,
    val recentChat: Chat?,
    val unReadMessageCount: Int,
)
