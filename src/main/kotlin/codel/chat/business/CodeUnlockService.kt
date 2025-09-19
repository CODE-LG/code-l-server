package codel.chat.business

import codel.chat.domain.*
import codel.chat.exception.UnlockException
import codel.chat.infrastructure.CodeUnlockRequestJpaRepository
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CodeUnlockService(
    private val codeUnlockRequestRepository: CodeUnlockRequestJpaRepository,
    private val chatRoomRepository: ChatRoomRepository,
    private val chatJpaRepository: ChatJpaRepository,
    private val policyService: CodeUnlockPolicyService,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository
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

        val currentRequest = codeUnlockRequestRepository.findLatestPendingByChatRoomId(chatRoom.getIdOrThrow()).firstOrNull()

        return UnlockInfo(
            isUnlocked = isUnlocked,
            currentRequest = currentRequest,
            canRequest = canRequest
        )
    }

    /**
     * 코드해제 요청 승인 (2단계)
     */
    fun approveUnlock(requestId: Long, processor: Member): CodeUnlockRequest {
        val unlockRequest = codeUnlockRequestRepository.findById(requestId)
            .orElseThrow { UnlockException(HttpStatus.BAD_REQUEST, "존재하지 않는 코드해제 요청입니다.") }

        // 권한 검증 - 요청자가 아닌 다른 채팅방 멤버만 승인 가능
        validateProcessor(unlockRequest, processor)

        // 승인 처리
        unlockRequest.approve(processor)

        // 승인 시스템 메시지 생성
        val systemMessage = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = unlockRequest.chatRoom,
                message = "코드해제가 승인되었습니다! 이제 서로의 프로필을 확인할 수 있어요.",
                chatContentType = ChatContentType.UNLOCKED_APPROVED
            )
        )

        unlockRequest.chatRoom.updateRecentChat(systemMessage)

        return unlockRequest
    }

    /**
     * 코드해제 요청 거절 (2단계)
     */
    fun rejectUnlock(requestId: Long, processor: Member): CodeUnlockRequest {
        val unlockRequest = codeUnlockRequestRepository.findById(requestId)
            .orElseThrow { IllegalArgumentException("존재하지 않는 코드해제 요청입니다.") }

        // 권한 검증 - 요청자가 아닌 다른 채팅방 멤버만 거절 가능
        validateProcessor(unlockRequest, processor)

        // 거절 처리
        unlockRequest.reject(processor)

        // 거절 시스템 메시지 생성
        val systemMessage = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = unlockRequest.chatRoom,
                message = "코드해제 요청이 거절되었습니다.",
                chatContentType = ChatContentType.UNLOCKED_REJECTED
            )
        )

        unlockRequest.chatRoom.updateRecentChat(systemMessage)
        unlockRequest.chatRoom.reject()

        return unlockRequest
    }

    /**
     * 처리자 권한 검증
     */
    private fun validateProcessor(unlockRequest: CodeUnlockRequest, processor: Member) {
        val chatRoomId = unlockRequest.chatRoom.getIdOrThrow()

        // 1. 요청자의 채팅방이 맞는지 확인
        chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, processor) ?: throw UnlockException(HttpStatus.BAD_REQUEST, "요청자의 채팅방을 찾을 수 없습니다.")
        // 2. 본인의 요청은 처리할 수 없음
        if (unlockRequest.requester.getIdOrThrow() == processor.getIdOrThrow()) {
            throw UnlockException(HttpStatus.BAD_REQUEST, "본인의 요청은 직접 처리할 수 없습니다.")
        }
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
