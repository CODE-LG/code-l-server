package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.CreateChatRoomResponse
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponses
import codel.config.argumentresolver.LoginMember
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class ChatController(
    private val chatService: ChatService,
    private val messageTemplate: SimpMessagingTemplate,
) {
    @PostMapping("/v1/chatroom")
    fun createChatRoom(
        @LoginMember requester: MemberEntity,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<CreateChatRoomResponse> {
        val createChatRoomResponse = chatService.createChatRoom(requester, request.partnerId)

        messageTemplate.convertAndSend("/sub/v1/chatroom/member", createChatRoomResponse)
        return ResponseEntity.ok(createChatRoomResponse)
    }

    @GetMapping("/v1/chatrooms")
    fun getChatRooms(
        @LoginMember requester: MemberEntity,
    ): ResponseEntity<ChatRoomResponses> {
        val chatRoomResponses = chatService.getChatRooms(requester)

        return ResponseEntity.ok(chatRoomResponses)
    }

    @GetMapping("/v1/chatroom/{chatRoomId}/chats")
    fun getChats(
        @LoginMember requester: MemberEntity,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<ChatResponses> {
        val chatResponses = chatService.getChats(chatRoomId, requester)

        return ResponseEntity.ok(chatResponses)
    }
}
