package codel.chat.domain

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany

@Entity
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var isActive: Boolean = true,
    @OneToMany(fetch = FetchType.EAGER)
    @JoinColumn(name = "chat_room_member_id")
    var chatRoomMembers: MutableSet<ChatRoomMember> = mutableSetOf(),
) {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")
}
