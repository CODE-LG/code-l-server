package codel.chat.infrastructure.entity

import codel.member.infrastructure.entity.MemberEntity
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne

@Entity
class ChatRoomMemberEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var chatRoomEntity: ChatRoomEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    var memberEntity: MemberEntity,
) {
    companion object {
        fun toEntity(
            chatRoomEntity: ChatRoomEntity,
            memberEntity: MemberEntity,
        ): ChatRoomMemberEntity =
            ChatRoomMemberEntity(
                chatRoomEntity = chatRoomEntity,
                memberEntity = memberEntity,
            )
    }

    fun toMemberDomain() = memberEntity.toDomain()
}
