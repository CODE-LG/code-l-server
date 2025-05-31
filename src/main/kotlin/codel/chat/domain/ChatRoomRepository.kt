package codel.chat.domain

import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.infrastructure.entity.ChatRoomEntity
import codel.chat.infrastructure.entity.ChatRoomMemberEntity
import codel.member.domain.Member
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
        requester: Member,
        partner: Member,
    ): ChatRoom {
        val chatRoomEntity = chatRoomJpaRepository.save(ChatRoomEntity.toEntity(chatRoom))

        val requesterMemberEntity = findMemberEntity(requester)
        val partnerMemberEntity = findMemberEntity(partner)

        saveChatRoomMember(chatRoomEntity, requesterMemberEntity, partnerMemberEntity)

        return chatRoomEntity.toDomain()
    }

    private fun findMemberEntity(member: Member) =
        memberJpaRepository.findById(member.getIdOrThrow()).orElseThrow { IllegalArgumentException() }

    private fun saveChatRoomMember(
        chatRoomEntity: ChatRoomEntity,
        requesterMemberEntity: MemberEntity,
        partnerMemberEntity: MemberEntity,
    ) {
        chatRoomMemberJpaRepository.save(ChatRoomMemberEntity.toEntity(chatRoomEntity, requesterMemberEntity))
        chatRoomMemberJpaRepository.save(ChatRoomMemberEntity.toEntity(chatRoomEntity, partnerMemberEntity))
    }
}
