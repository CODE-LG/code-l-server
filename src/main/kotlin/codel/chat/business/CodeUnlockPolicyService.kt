package codel.chat.business

import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomStatus
import codel.chat.exception.ChatException
import codel.chat.infrastructure.CodeUnlockRequestJpaRepository
import codel.member.domain.Member
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class CodeUnlockPolicyService(
    private val codeUnlockRequestRepository: CodeUnlockRequestJpaRepository
) {

    /**
     * 코드해제 요청 가능 여부 검증
     */
    fun validateCanRequest(chatRoom: ChatRoom, requester: Member) {
        // 1. 이미 해제된 경우
        if (chatRoom.isUnlocked) {
            throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드가 해제된 채팅방입니다.")
        }

        // 2. 진행 중인 요청이 있는 경우
        val existingRequest = codeUnlockRequestRepository.findPendingRequestByRequester(
            chatRoom.getIdOrThrow(), 
            requester
        )
        if (existingRequest != null) {
            throw ChatException(HttpStatus.BAD_REQUEST, "이미 코드해제 요청을 보낸 상태입니다.")
        }

        // 3. 채팅방 상태 검증
        if (chatRoom.status == ChatRoomStatus.DISABLED) {
            throw ChatException(HttpStatus.BAD_REQUEST, "사용할 수 없는 채팅방입니다.")
        }
    }

    /**
     * 요청 가능 여부 확인 (예외 발생 없음)
     */
    fun canRequest(chatRoom: ChatRoom, requester: Member): Boolean {
        return try {
            validateCanRequest(chatRoom, requester)
            true
        } catch (e: ChatException) {
            false
        }
    }
}
