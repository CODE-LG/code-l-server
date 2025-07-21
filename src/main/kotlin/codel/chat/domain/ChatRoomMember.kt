package codel.chat.domain

import codel.member.domain.Member
import jakarta.persistence.*

@Entity
@Table(uniqueConstraints = [UniqueConstraint(columnNames = ["chat_room_id", "member_id"])])
class ChatRoomMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,
    @ManyToOne(optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,
    @OneToOne
    @JoinColumn(name = "chat_id")
    var lastReadChat: Chat? = null,
)
