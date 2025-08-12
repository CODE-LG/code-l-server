package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
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
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", chatRoomAndChatResponse.chatResponse)
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            chatRoomAndChatResponse.chatRoomResponse,
        )
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${chatRoomAndChatResponse.partner.getIdOrThrow()}",
            chatRoomAndChatResponse.chatRoomResponse,
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
        
        // 2. 채팅방 멤버들의 채팅방 목록 업데이트 (홈 화면에 있는 사용자들에게)
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${result.partner.getIdOrThrow()}",
            result.updatedChatRoom,
        )

        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
            result.updatedChatRoom,
        )
        return ResponseEntity.ok(result.chatResponse)
    }
}
