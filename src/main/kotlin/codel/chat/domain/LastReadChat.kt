package codel.chat.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["chat_room_member_id"]),
    ],
)
class LastReadChat(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_member_id", nullable = false)
    var chatRoomMember: ChatRoomMember,
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    var chat: Chat,
)
