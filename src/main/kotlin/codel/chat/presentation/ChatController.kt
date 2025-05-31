package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.dto.ChatRoomResponses
import codel.chat.presentation.dto.CreateChatRoomRequest
import codel.chat.presentation.dto.CreateChatRoomResponse
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/v1/chat")
    fun createChatRoom(
        @LoginMember requester: Member,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<CreateChatRoomResponse> {
        val createChatRoomResponse = chatService.createChatRoom(requester, request.partnerId)

        return ResponseEntity.ok(createChatRoomResponse)
    }

    @GetMapping("/v1/chatrooms")
    fun getChatRooms(
        @LoginMember member: Member,
    ): ResponseEntity<ChatRoomResponses> {
        val chatRoomResponses = chatService.getChatRooms(member)

        return ResponseEntity.ok(chatRoomResponses)
    }
}
