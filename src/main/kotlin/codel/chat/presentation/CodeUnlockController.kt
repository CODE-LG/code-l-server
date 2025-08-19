package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.business.CodeUnlockService
import codel.chat.presentation.response.UnlockRequestResponse
import codel.chat.presentation.swagger.CodeUnlockControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*

@RestController
class CodeUnlockController(
    private val codeUnlockService: CodeUnlockService,
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate
) : CodeUnlockControllerSwagger{

    /**
     * 코드해제 요청 (1단계)
     */
    @PostMapping("/v1/chatroom/{chatRoomId}/unlock/request")
    override fun requestUnlock(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long
    ): ResponseEntity<UnlockRequestResponse> {
        
        // 코드해제 요청 처리
        val unlockRequest = codeUnlockService.requestUnlock(chatRoomId, requester)
        
        // 채팅방 업데이트 정보 가져오기 (기존 ChatService 활용)
        val chatRoomAndChatResponse = chatService.updateUnlockChatRoom(requester, chatRoomId)
        
        // 실시간 알림 전송
        // 1. 채팅방 실시간 메시지 전송
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", chatRoomAndChatResponse.chatResponse)
        
        // 2. 발송자에게는 본인용 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            chatRoomAndChatResponse.requesterChatRoomResponse,
        )
        
        // 3. 상대방에게는 읽지 않은 수가 증가된 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${chatRoomAndChatResponse.partner.getIdOrThrow()}",
            chatRoomAndChatResponse.partnerChatRoomResponse,
        )

        return ResponseEntity.ok(UnlockRequestResponse.from(unlockRequest))
    }
}
