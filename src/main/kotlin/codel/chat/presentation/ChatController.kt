package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.swagger.ChatControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import jdk.internal.joptsimple.internal.Messages.message
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
    @PostMapping("/v1/chatroom")
    override fun createChatRoom(
        @LoginMember requester: Member,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<ChatRoomResponse> {
        val response = chatService.createChatRoom(requester, request)

        messagingTemplate.convertAndSend("/sub/v1/chatroom/member${request.partnerId}", response)
        return ResponseEntity.ok(response)
    }

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
        @RequestBody chatLogRequest : ChatLogRequest,
        @PageableDefault(size = 30, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>> {
        val chatResponses = chatService.getChats(chatRoomId, chatLogRequest.lastChatId, requester, pageable)

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
    fun requestUnlockChatRoomStatus(
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
            "/sub/v1/chatroom/member/${requester.id}",
            chatRoomAndChatResponse.chatRoomResponse,
        )

        // TODO : 실시간 채팅 도중 상대방에게 실시간으로 바텀시트 올라가게끔 알려줄 수 있는 이벤트 발행
        //  messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId/events")
        return ResponseEntity.ok().build()
    }
}
