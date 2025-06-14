package codel.chat.presentation

import codel.chat.presentation.request.ChatRequest
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
) {
    @MessageMapping("/v1/chatroom/{chatRoomId}")
    fun sendChat(
        @LoginMember requester: Member,
        @DestinationVariable("chatRoomId") chatRoomId: Long,
        @Payload chatRequest: ChatRequest,
    ) {
        messagingTemplate.convertAndSend("/sub/v1/chatroom/member", chatRequest)
        messagingTemplate.convertAndSend("/sub/v1/chatroom/1", chatRequest)
    }
}
