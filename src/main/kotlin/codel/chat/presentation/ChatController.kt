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
    private val messageTemplate: SimpMessagingTemplate,
) : ChatControllerSwagger {
    @PostMapping("/v1/chatroom")
    override fun createChatRoom(
        @LoginMember requester: Member,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<ChatRoomResponse> {
        val response = chatService.createChatRoom(requester, request)

        messageTemplate.convertAndSend("/sub/v1/chatroom/member${request.partnerId}", response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/chatrooms")
    override fun getChatRooms(
        @LoginMember requester: Member,
        @PageableDefault(size = 10, page = 0) pageable: Pageable,
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
        chatService.updateLastChat(chatRoomId, chatLogRequest, requester)

        return ResponseEntity.noContent().build()
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/unlock")
    fun updateChatRoomStatus(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit> {
        chatService.updateUnlockChatRoom(requester, chatRoomId)
        return ResponseEntity.ok().build()
    }
}
