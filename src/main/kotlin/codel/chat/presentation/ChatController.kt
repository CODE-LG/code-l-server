package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.dto.CreateChatRoomRequest
import codel.chat.presentation.dto.CreateChatRoomResponse
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class ChatController(
    private val chatService: ChatService,
) {
    @PostMapping("/api/v1/chat")
    fun createChatRoom(
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<CreateChatRoomResponse> {
        val createChatRoomResponse = chatService.createChatRoom(request.creatorId, request.partnerId)

        return ResponseEntity.ok(createChatRoomResponse)
    }
}
