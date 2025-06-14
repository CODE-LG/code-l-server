package codel.chat.domain

import codel.member.infrastructure.entity.MemberEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class ChatRoomMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    var chatRoom: ChatRoom,
    @ManyToOne
    var memberEntity: MemberEntity,
)
