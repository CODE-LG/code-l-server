package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponses
import codel.chat.presentation.response.CreateChatRoomResponse
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
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
) {
    @PostMapping("/v1/chatroom")
    fun createChatRoom(
        @LoginMember requester: Member,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<CreateChatRoomResponse> {
        val response = chatService.createChatRoom(requester, request)

        messageTemplate.convertAndSend("/sub/v1/chatroom/member${request.partnerId}", response)
        return ResponseEntity.ok(response)
    }

    @GetMapping("/v1/chatrooms")
    fun getChatRooms(
        @LoginMember requester: Member,
    ): ResponseEntity<ChatRoomResponses> {
        val chatRoomResponses = chatService.getChatRooms(requester)

        return ResponseEntity.ok(chatRoomResponses)
    }

    @GetMapping("/v1/chatroom/{chatRoomId}/chats")
    fun getChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<ChatResponses> {
        val chatResponses = chatService.getChats(chatRoomId, requester)

        return ResponseEntity.ok(chatResponses)
    }

    @PutMapping("/v1/chatroom/{chatRoomId}/last-chat")
    fun updateLastChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody updateLastChatRequest: UpdateLastChatRequest,
    ): ResponseEntity<Unit> {
        chatService.updateLastChat(chatRoomId, updateLastChatRequest, requester)

        return ResponseEntity.ok().build()
    }
}
