package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
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
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody

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

    @GetMapping("/v1/chatRooms/{chatRoomId}")
    override fun getChatRoom(
        @PathVariable chatRoomId: Long,
        @LoginMember requester: Member,
    ): ResponseEntity<ChatRoomResponse> {
        val chatRoomResponse = chatService.getChatRoom(chatRoomId, requester)
        return ResponseEntity.ok(chatRoomResponse)
    }

    @GetMapping("/v1/chatroom/{chatRoomId}/chats")
    override fun getChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @PageableDefault(size = 20, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>> {
        val chatResponses = chatService.getChats(chatRoomId, requester, pageable)

        return ResponseEntity.ok(chatResponses)
    }

    @PutMapping("/v1/chatroom/{chatRoomId}/last-chat")
    override fun updateLastChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody updateLastChatRequest: UpdateLastChatRequest,
    ): ResponseEntity<Unit> {
        chatService.updateLastChat(chatRoomId, updateLastChatRequest, requester)

        return ResponseEntity.ok().build()
    }
}
