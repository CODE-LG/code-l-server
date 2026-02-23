package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.exception.ChatException
import codel.chat.presentation.request.ChatSendRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatErrorResponse
import codel.chat.presentation.response.ChatErrorType
import codel.config.argumentresolver.LoginMember
import codel.config.exception.CodelException
import codel.config.Loggable
import codel.config.RedisMessageRelay
import codel.member.domain.Member
import org.springdoc.webmvc.core.service.RequestService
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val redisMessageRelay: RedisMessageRelay,
    private val chatService: ChatService,
    private val requestService: RequestService,
) : Loggable {
    @MessageMapping("/v1/chatroom/{chatRoomId}/chat")
    fun sendChat(
        @DestinationVariable("chatRoomId") chatRoomId: Long,
        @LoginMember requester: Member,
        @Payload chatSendRequest: ChatSendRequest,
    ) {
        try {
            // 메시지 전송 가능 여부 확인
            chatService.validateCanSendMessage(chatRoomId, requester)

            val responseDto = chatService.saveChat(chatRoomId, requester, chatSendRequest)

            // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/member/${responseDto.partner.id}",
                responseDto.partnerChatRoomResponse,
            )

            // 발송자에게는 본인 기준 채팅방 정보 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/member/${requester.id}",
                responseDto.requesterChatRoomResponse,
            )

            // 채팅방 구독자들에게 실시간 메시지 전송
            redisMessageRelay.publish("/sub/v1/chatroom/$chatRoomId", responseDto.chatResponse)

        } catch (e: ChatException) {
            // 채팅 관련 예외 처리
            log.warn { "❌ 메시지 전송 실패 - chatRoomId: $chatRoomId, memberId: ${requester.id}, 사유: ${e.message}" }
            sendErrorToClient(requester.id!!, chatRoomId, e.message, ChatErrorType.VALIDATION_ERROR)

        } catch (e: CodelException) {
            // 기타 도메인 예외 처리
            log.warn { "❌ 메시지 전송 실패 - chatRoomId: $chatRoomId, memberId: ${requester.id}, 사유: ${e.message}" }
            sendErrorToClient(requester.id!!, chatRoomId, e.message, ChatErrorType.PERMISSION_DENIED)

        } catch (e: Exception) {
            // 예상치 못한 예외 처리
            log.error(e) { "💥 메시지 저장 중 예외 발생 - chatRoomId: $chatRoomId, memberId: ${requester.id}" }
            sendErrorToClient(requester.id!!, chatRoomId, "메시지 전송에 실패했습니다. 잠시 후 다시 시도해주세요.", ChatErrorType.INTERNAL_ERROR)
        }
    }

    @MessageMapping("/v1/chatroom/{chatRoomId}")
    fun readChat(
        @DestinationVariable("chatRoomId") chatRoomId: Long,
        @LoginMember requester: Member,
        @Payload updateLastChatRequest: UpdateLastChatRequest,
    ) {
        try {
            chatService.updateLastChat(chatRoomId, updateLastChatRequest.lastChatId, requester)

        } catch (e: ChatException) {
            log.warn { "❌ 읽음 처리 실패 - chatRoomId: $chatRoomId, memberId: ${requester.id}, 사유: ${e.message}" }
            sendErrorToClient(requester.id!!, chatRoomId, e.message, ChatErrorType.VALIDATION_ERROR)

        } catch (e: CodelException) {
            log.warn { "❌ 읽음 처리 실패 - chatRoomId: $chatRoomId, memberId: ${requester.id}, 사유: ${e.message}" }
            sendErrorToClient(requester.id!!, chatRoomId, e.message, ChatErrorType.PERMISSION_DENIED)

        } catch (e: Exception) {
            log.error(e) { "💥 읽음 처리 중 예외 발생 - chatRoomId: $chatRoomId, memberId: ${requester.id}" }
            sendErrorToClient(requester.id!!, chatRoomId, "읽음 처리에 실패했습니다.", ChatErrorType.INTERNAL_ERROR)
        }
    }

    /**
     * 클라이언트에게 에러 메시지 전송
     */
    private fun sendErrorToClient(
        memberId: Long,
        chatRoomId: Long?,
        message: String,
        errorType: ChatErrorType
    ) {
        val errorResponse = ChatErrorResponse(
            errorType = errorType,
            message = message,
            chatRoomId = chatRoomId
        )

        // 해당 사용자에게만 에러 메시지 전송
        redisMessageRelay.publish(
            "/sub/v1/chatroom/member/$memberId/errors",
            errorResponse
        )
    }
}
