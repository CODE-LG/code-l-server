package codel.chat.repository

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatSendRequest
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class ChatRepository(
    private val chatJpaRepository: ChatJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
) {
    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatSendRequest: ChatSendRequest,
    ): Chat {
        val requesterChatRoomMember = findMe(chatRoomId, requester)

        return chatJpaRepository.save(Chat.of(requesterChatRoomMember, chatSendRequest))
    }

    fun saveDateChat(chatRoom: ChatRoom, dateMessage: String) {
        chatJpaRepository.save(Chat.createSystemMessage(chatRoom = chatRoom, message = dateMessage, chatContentType = ChatContentType.TIME))
    }

    fun findNextChats(
        chatRoomId: Long,
        lastChatId: Long?,
        pageable: Pageable,
    ): Page<Chat> {
        val pageableWithSort: Pageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, getChatDefaultSort())
        val chatRoom =
            chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: throw ChatException(
                HttpStatus.BAD_REQUEST,
                "채팅방을 찾을 수 없습니다.",
            )

        if(lastChatId == null){
            return chatJpaRepository.findNextChats(chatRoom, pageableWithSort)
        }

        return chatJpaRepository.findNextChats(chatRoom, lastChatId, pageableWithSort)
    }

    fun findPrevChats(
        chatRoomId: Long,
        lastChatId: Long?,
        pageable: Pageable,
    ): Page<Chat> {
        val pageableWithSort: Pageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, getChatDefaultSort())
        val chatRoom =
            chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: throw ChatException(
                HttpStatus.BAD_REQUEST,
                "채팅방을 찾을 수 없습니다.",
            )

        if(lastChatId == null){
            throw ChatException(HttpStatus.NO_CONTENT, "이전 채팅이 존재하지 않습니다.")
        }

        return chatJpaRepository.findPrevChats(chatRoom, lastChatId, pageableWithSort)
    }

    fun findChat(chatId: Long): Chat =
        chatJpaRepository.findByIdOrNull(chatId) ?: throw ChatException(
            HttpStatus.BAD_REQUEST,
            "해당 chatId에 맞는 채팅을 찾을 수 없습니다.",
        )

    fun upsertLastChat(
        chatRoomId: Long,
        requester: Member,
        chat: Chat,
    ) {
        val requesterChatRoomMember = findMe(chatRoomId, requester)
        requesterChatRoomMember.lastReadChat = chat
        chatRoomMemberJpaRepository.save(requesterChatRoomMember)
    }

    fun getUnReadMessageCount(
        chatRoom: ChatRoom,
        requester: Member,
    ): Int {
        val requesterChatRoomMember =
            chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)
                ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방에 속해있는 사용자가 아닙니다.")

        if(requesterChatRoomMember.lastReadChat == null){
            return chatJpaRepository.countByChatRoomAfterLastChat(chatRoom, requester)
        }

        return chatJpaRepository.countByChatRoomAfterLastChat(
            chatRoom,
            requesterChatRoomMember.lastReadChat!!.getSentAtOrThrow(),
            requester,
        )
    }

    /**
     * 채팅방에서 메시지 전송 가능 여부 확인
     */
    fun canSendMessage(chatRoomId: Long, sender: Member): Boolean {
        val chatRoom = chatRoomJpaRepository.findByIdOrNull(chatRoomId) ?: return false
        val allMembers = chatRoomMemberJpaRepository.findByChatRoomId(chatRoomId)

        // 발송자가 활성 상태가 아니면 전송 불가
        val senderMember = allMembers.find { it.member == sender }
        if (senderMember?.isActive() != true) return false
        
        // 상대방이 존재하고 활성 상태인지 확인
        val partner = allMembers.find { it.member != sender }
        return partner?.isActive() == true
    }

    private fun findMe(
        chatRoomId: Long,
        requester: Member,
    ): ChatRoomMember =
        chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, requester)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방 멤버가 존재하지 않습니다.")
    private fun getChatDefaultSort(): Sort = Sort.by(Sort.Order.desc("sentAt"))
}
