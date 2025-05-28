package codel.chat.domain

import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.infrastructure.entity.ChatRoomEntity
import codel.chat.infrastructure.entity.ChatRoomMemberEntity
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class ChatRoomRepository(
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val memberJpaRepository: MemberJpaRepository,
) {
    fun saveChatRoom(
        chatRoom: ChatRoom,
        creatorId: Long,
        partnerId: Long,
    ): ChatRoom {
        val chatRoomEntity = chatRoomJpaRepository.save(ChatRoomEntity.toEntity(chatRoom))
        saveChatRoomMember(chatRoomEntity, findMemberById(creatorId))
        saveChatRoomMember(chatRoomEntity, findMemberById(partnerId))

        return chatRoomEntity.toDomain()
    }

    private fun findMemberById(creatorId: Long): MemberEntity =
        memberJpaRepository
            .findById(creatorId)
            .orElseThrow { IllegalArgumentException("해당 ID의 사용자가 존재하지 않습니다. id=$creatorId") }

    private fun saveChatRoomMember(
        chatRoomEntity: ChatRoomEntity,
        memberEntity: MemberEntity,
    ) = chatRoomMemberJpaRepository.save(
        ChatRoomMemberEntity(
            chatRoomEntity = chatRoomEntity,
            memberEntity = memberEntity,
        ),
    )
}
