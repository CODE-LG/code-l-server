package codel.chat.infrastructure.entity

import codel.chat.domain.ChatRoom
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
class ChatRoomEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var isActive: Boolean = true,
) {
    companion object {
        fun toEntity(chatRoom: ChatRoom): ChatRoomEntity =
            ChatRoomEntity(
                id = chatRoom.id,
                isActive = chatRoom.isActive,
            )
    }

    fun toDomain(): ChatRoom =
        ChatRoom(
            id = this.id,
            isActive = this.isActive,
        )
}
