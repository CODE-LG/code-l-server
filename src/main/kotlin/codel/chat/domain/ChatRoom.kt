package codel.chat.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import java.time.LocalDateTime

@Entity
class ChatRoom(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    var isActive: Boolean = true,
    @OneToOne
    @JoinColumn(name = "chat_id")
    var recentChat: Chat? = null,
    var createdAt: LocalDateTime? = null,
    var updatedAt: LocalDateTime? = null,
) {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방이 존재하지 않습니다.")
}
