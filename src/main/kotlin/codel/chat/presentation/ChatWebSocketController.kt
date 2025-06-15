package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.presentation.request.ChatRequest
import codel.config.argumentresolver.LoginMember
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class ChatWebSocketController(
    private val messagingTemplate: SimpMessagingTemplate,
    private val chatService: ChatService,
) {
    @MessageMapping("/v1/chatroom/{chatRoomId}")
    fun sendChat(
        @DestinationVariable("chatRoomId") chatRoomId: Long,
        @LoginMember requester: MemberEntity,
        @Payload chatRequest: ChatRequest,
    ) {
        val chatRoomAndChatResponse = chatService.saveChat(chatRoomId, requester, chatRequest)

        messagingTemplate.convertAndSend("/sub/v1/chatroom/member", chatRoomAndChatResponse.first)
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", chatRoomAndChatResponse.second)
    }
}
