package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.business.CodeUnlockService
import codel.chat.presentation.response.UnlockRequestResponse
import codel.chat.presentation.swagger.CodeUnlockControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.*

@RestController
class CodeUnlockController(
    private val codeUnlockService: CodeUnlockService,
    private val chatService: ChatService,
    private val memberService : MemberService,
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

    /**
     * 코드해제 요청 승인 (2단계)
     */
    @PutMapping("/v1/unlock-request/{requestId}/approve")
    override fun approveUnlock(
        @LoginMember processor: Member,
        @PathVariable requestId: Long
    ): ResponseEntity<UnlockRequestResponse> {
        
        // 코드해제 승인 처리
        val approvedRequest = codeUnlockService.approveUnlock(requestId, processor)
        
        // 승인된 채팅방 정보 조회
        val chatRoomId = approvedRequest.chatRoom.getIdOrThrow()
        val requester = memberService.findMember(approvedRequest.requester.getIdOrThrow())

        // 실시간 알림 전송 (승인 메시지)
        sendUnlockProcessNotification(chatRoomId, processor, requester, "approved")
        
        return ResponseEntity.ok(UnlockRequestResponse.from(approvedRequest))
    }

    /**
     * 코드해제 요청 거절 (2단계)
     */
    @PutMapping("/v1/unlock-request/{requestId}/reject")
    override fun rejectUnlock(
        @LoginMember processor: Member,
        @PathVariable requestId: Long
    ): ResponseEntity<UnlockRequestResponse> {
        
        // 코드해제 거절 처리
        val rejectedRequest = codeUnlockService.rejectUnlock(requestId, processor)
        
        // 거절된 채팅방 정보 조회
        val chatRoomId = rejectedRequest.chatRoom.getIdOrThrow()
        val requester = memberService.findMember(rejectedRequest.requester.getIdOrThrow())
        

        // 실시간 알림 전송 (거절 메시지)
        sendUnlockProcessNotification(chatRoomId, processor, requester, "rejected")
        
        return ResponseEntity.ok(UnlockRequestResponse.from(rejectedRequest, requester))
    }

    /**
     * 승인/거절 시 실시간 알림 전송
     */
    private fun sendUnlockProcessNotification(
        chatRoomId: Long,
        processor: Member,
        requester: Member,
        action: String
    ) {
        // 최신 채팅방 정보 조회 (승인/거절 후 상태 반영)
        val chatRoom = chatService.findChatRoomById(chatRoomId)
        val recentChat = chatRoom.recentChat
        
        if (recentChat != null) {
            // 1. 채팅방 실시간 메시지 전송
            messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", 
                chatService.buildChatResponse(processor, recentChat))
            
            // 2. 처리자(승인/거절한 사람)에게 채팅방 업데이트 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${processor.getIdOrThrow()}",
                chatService.buildChatRoomResponse(chatRoom, processor, requester)
            )
            
            // 3. 요청자에게 채팅방 업데이트 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
                chatService.buildChatRoomResponse(chatRoom, requester, processor)
            )
        }
    }
}
