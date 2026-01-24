package codel.chat.presentation

import codel.chat.business.ChatService
import codel.chat.business.strategy.QuestionRecommendStrategyResolver
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.request.ChatSendRequest
import codel.chat.presentation.request.QuestionRecommendRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomEventType
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.QuestionRecommendResponseV2
import codel.chat.presentation.swagger.ChatControllerSwagger
import codel.config.Loggable
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
    private val strategyResolver: QuestionRecommendStrategyResolver
) : ChatControllerSwagger, Loggable {
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

    @GetMapping("/v1/chatroom/{chatRoomId}/chats/previous")
    override fun getPreviousChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestParam(required = false) lastReadChatId: Long?,
        @PageableDefault(size = 30, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>> {
        val chatResponses = chatService.getPreviousChats(chatRoomId, lastReadChatId, requester, pageable)

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

    @PostMapping("/v1/chatroom/{chatRoomId}/questions/random")
    override fun sendRandomQuestion(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long
    ): ResponseEntity<ChatResponse> {
        val result = chatService.sendRandomQuestion(chatRoomId, requester)
        
        // 1. 채팅방 실시간 메시지 전송 (채팅방에 있는 사용자들에게)
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", result.chatResponse)
        
        // 2. 발송자에게는 본인용 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
            result.requesterChatRoomResponse,
        )
        
        // 3. 상대방에게는 읽지 않은 수가 증가된 채팅방 응답 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${result.partner.getIdOrThrow()}",
            result.partnerChatRoomResponse,
        )
        return ResponseEntity.ok(result.chatResponse)
    }

    /**
     * 질문 추천 API (버전 분기)
     *
     * - 1.3.0 이상: 카테고리 기반 질문 추천 (CategoryBasedQuestionStrategy)
     * - 1.3.0 미만: 기존 랜덤 질문 추천 (LegacyRandomQuestionStrategy)
     */
    @PostMapping("/v1/chatroom/{chatRoomId}/questions/recommend")
    override fun recommendQuestion(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestHeader(value = "X-App-Version", required = false) appVersion: String?,
        @RequestBody request: QuestionRecommendRequest
    ): ResponseEntity<Any> {
        log.info { "질문 추천 요청 - chatRoomId: $chatRoomId, appVersion: $appVersion, category: ${request.category}" }

        val strategy = strategyResolver.resolveStrategy(appVersion)
        val response = strategy.recommendQuestion(chatRoomId, requester, request)

        // V2 응답인 경우 WebSocket 메시지 전송
        if (response.body is QuestionRecommendResponseV2) {
            val v2Response = response.body as QuestionRecommendResponseV2
            if (v2Response.success && v2Response.chat != null) {
                // 채팅방 실시간 메시지 전송
                messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", v2Response.chat.chatResponse)

                // 발송자에게 채팅방 응답 전송
                messagingTemplate.convertAndSend(
                    "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
                    v2Response.chat.requesterChatRoomResponse
                )

                // 상대방에게 채팅방 응답 전송
                messagingTemplate.convertAndSend(
                    "/sub/v1/chatroom/member/${v2Response.chat.partner.getIdOrThrow()}",
                    v2Response.chat.partnerChatRoomResponse
                )
            }
        }

        return response
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/chat")
    fun sendChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody chatSendRequest: ChatSendRequest,
    ): ResponseEntity<ChatResponse> {
        // 메시지 전송 가능 여부 확인
        chatService.validateCanSendMessage(chatRoomId, requester)
        
        val responseDto = chatService.saveChat(chatRoomId, requester, chatSendRequest)

        // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${responseDto.partner.id}",
            responseDto.partnerChatRoomResponse,
        )

        // 발송자에게는 본인 기준 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            responseDto.requesterChatRoomResponse,
        )

        // 채팅방 구독자들에게 실시간 메시지 전송
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", responseDto.chatResponse)
        return ResponseEntity.ok(responseDto.chatResponse)
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/leave")
    override fun leaveChatRoom(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit> {
        val requesterChatRoomResponse = chatService.leaveChatRoom(chatRoomId, requester)

        // 본인에게 채팅방 삭제 이벤트 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            requesterChatRoomResponse.copy(eventType = ChatRoomEventType.REMOVED),
        )
        
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/chatroom/{chatRoomId}/close")
    override fun closeConversationAtChatRoom(
        @LoginMember requester : Member,
        @PathVariable chatRoomId: Long,
    ) : ResponseEntity<Unit> {
        val responseDto = chatService.closeConversation(chatRoomId, requester)

        // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${responseDto.partner.id}",
            responseDto.partnerChatRoomResponse,
        )

        // 발송자에게는 본인 기준 채팅방 정보 전송
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.id}",
            responseDto.requesterChatRoomResponse,
        )

        // 채팅방 구독자들에게 실시간 메시지 전송
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", responseDto.chatResponse)
        return ResponseEntity.ok().build()
    }
}
