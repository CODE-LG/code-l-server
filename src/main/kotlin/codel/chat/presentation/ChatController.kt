package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.request.ChatSendRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.swagger.ChatControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@Controller
class ChatController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate,
) : ChatControllerSwagger {
    @GetMapping("/v1/chatrooms")
    override fun getChatRooms(
        @LoginMember requester: Member,
        @PageableDefault(size = 20000, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatRoomResponse>> {
        val chatRoomResponses = chatService.getChatRooms(requester, pageable)
        return ResponseEntity.ok(chatRoomResponses)
    }

    @GetMapping("/v1/chatroom/{chatRoomId}/chats")
    override fun getChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestParam(required = false) lastReadChatId: Long?,
        @PageableDefault(size = 30, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>> {
        val chatResponses = chatService.getChats(chatRoomId, lastReadChatId, requester, pageable)

        return ResponseEntity.ok(chatResponses)
    }

    @GetMapping("/v1/chatroom/{chatRoomId}/chats/previous")
    fun getPreviousChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestParam(required = false) lastReadChatId: Long?,
        @PageableDefault(size = 30, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>> {
        val chatResponses = chatService.getPreviousChats(chatRoomId, lastReadChatId, requester, pageable)

        return ResponseEntity.ok(chatResponses)
    }

    @PutMapping("/v1/chatroom/{chatRoomId}/last-chat")
    override fun updateLastChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody chatLogRequest: ChatLogRequest,
    ): ResponseEntity<Unit> {
        chatService.updateLastChat(chatRoomId, chatLogRequest.lastChatId, requester)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/unlock")
    override fun updateChatRoomStatus(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit> {
        val chatRoomAndChatResponse = chatService.updateUnlockChatRoom(requester, chatRoomId)
        
        // 채팅방 실시간 메시지 전송
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", chatRoomAndChatResponse.chatResponse)
        
        // 발송자에게는 본인용 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            chatRoomAndChatResponse.requesterChatRoomResponse,
        )
        
        // 상대방에게는 읽지 않은 수가 증가된 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${chatRoomAndChatResponse.partner.getIdOrThrow()}",
            chatRoomAndChatResponse.partnerChatRoomResponse,
        )

        // TODO : 실시간 채팅 도중 상대방에게 실시간으로 바텀시트 올라가게끔 알려줄 수 있는 이벤트 발행
        //  messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId/events")
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/questions/random")
    override fun sendRandomQuestion(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long
    ): ResponseEntity<ChatResponse> {
        val result = chatService.sendRandomQuestion(chatRoomId, requester)
        
        // 1. 채팅방 실시간 메시지 전송 (채팅방에 있는 사용자들에게)
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", result.chatResponse)
        
        // 2. 발송자에게는 본인용 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
            result.requesterChatRoomResponse,
        )
        
        // 3. 상대방에게는 읽지 않은 수가 증가된 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${result.partner.getIdOrThrow()}",
            result.partnerChatRoomResponse,
        )
        
        return ResponseEntity.ok(result.chatResponse)
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/chat")
    fun sendChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody chatSendRequest: ChatSendRequest,
    ): ResponseEntity<ChatResponse> {
        val responseDto = chatService.saveChat(chatRoomId, requester, chatSendRequest)

        // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${responseDto.partner.id}",
            responseDto.partnerChatRoomResponse,
        )

        // 발송자에게는 본인 기준 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            responseDto.requesterChatRoomResponse,
        )

        // 채팅방 구독자들에게 실시간 메시지 전송
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", responseDto.chatResponse)
        return ResponseEntity.ok(responseDto.chatResponse)
    }
}
