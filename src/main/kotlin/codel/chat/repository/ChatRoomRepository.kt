package codel.chat.repository

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRoomRepository(
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
) {
    fun saveChatRoom(): ChatRoom = chatRoomJpaRepository.save(ChatRoom())

    fun saveChatRoomMember(
        chatRoom: ChatRoom,
        member: Member,
    ): ChatRoomMember = chatRoomMemberJpaRepository.save(ChatRoomMember(chatRoom = chatRoom, member = member))

    fun findAllChatRoomMembers(member: Member): List<ChatRoomMember> = chatRoomMemberJpaRepository.findByMember(member)

    fun findPartner(
        chatRoom: ChatRoom,
        requester: Member,
    ): ChatRoomMember =
        chatRoomMemberJpaRepository.findByChatRoomAndMemberNot(chatRoom, requester)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "채팅방에 자신을 제외한 다른 사용자가 존재하지 않습니다.")

    fun findChatRoomById(chatRoomId: Long): ChatRoom =
        chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: throw ChatException(
            HttpStatus.BAD_REQUEST,
            "chatId에 해당하는 채팅방을 찾을 수 없습니다.",
        )

    fun findMe(
        chatRoom: ChatRoom,
        member: Member,
    ): ChatRoomMember =
        chatRoomMemberJpaRepository.findByChatRoomAndMember(chatRoom, member)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방 멤버가 존재하지 않습니다.")
}
