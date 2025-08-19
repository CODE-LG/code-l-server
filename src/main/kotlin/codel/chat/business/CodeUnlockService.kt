package codel.chat.business

import codel.chat.domain.*
import codel.chat.infrastructure.CodeUnlockRequestJpaRepository
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CodeUnlockService(
    private val codeUnlockRequestRepository: CodeUnlockRequestJpaRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatJpaRepository: ChatJpaRepository,
    private val policyService: CodeUnlockPolicyService
) {

    /**
     * 코드해제 요청 (1단계)
     */
    fun requestUnlock(chatRoomId: Long, requester: Member): CodeUnlockRequest {
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        
        // 정책 검증
        policyService.validateCanRequest(chatRoom, requester)
        
        // 요청 생성
        val unlockRequest = CodeUnlockRequest(
            chatRoom = chatRoom,
            requester = requester
        )
        
        val savedRequest = codeUnlockRequestRepository.save(unlockRequest)
        
        // 시스템 메시지 생성
        val systemMessage = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = chatRoom,
                message = "코드해제 요청이 왔습니다.",
                chatContentType = ChatContentType.UNLOCKED_REQUEST
            )
        )
        
        chatRoom.updateRecentChat(systemMessage)
        
        return savedRequest
    }

    /**
     * 채팅방의 현재 해제 정보 조회 (1단계)
     */
    @Transactional(readOnly = true)
    fun getUnlockInfo(chatRoom: ChatRoom, requester: Member): UnlockInfo {
        val isUnlocked = chatRoom.isUnlocked
        val canRequest = policyService.canRequest(chatRoom, requester)
        
        val currentRequest = if (!isUnlocked) {
            codeUnlockRequestRepository.findLatestByChatRoomId(chatRoom.getIdOrThrow())
                ?.takeIf { it.isPending() }
        } else null
        
        return UnlockInfo(
            isUnlocked = isUnlocked,
            currentRequest = currentRequest,
            canRequest = canRequest
        )
    }
}

/**
 * 해제 정보 데이터 클래스
 */
data class UnlockInfo(
    val isUnlocked: Boolean,
    val currentRequest: CodeUnlockRequest?,
    val canRequest: Boolean
)
