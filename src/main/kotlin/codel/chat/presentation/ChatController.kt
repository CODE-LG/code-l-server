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
import codel.chat.presentation.response.QuestionSendResult
import codel.chat.presentation.response.SavedChatDto
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

    /**
     * 질문 추천 API (버전 분기)
     *
     * - 1.3.0 이상: 카테고리 기반 질문 추천 (CategoryBasedQuestionStrategy)
     * - 1.3.0 미만: 기존 랜덤 질문 추천 (LegacyRandomQuestionStrategy)
     */
    @PostMapping("/v1/chatroom/{chatRoomId}/questions/random")
    override fun sendRandomQuestion(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestHeader(value = "X-App-Version", required = false) appVersion: String?,
        @RequestBody(required = false) request: QuestionRecommendRequest?
    ): ResponseEntity<Any> {
        log.info { "질문 추천 요청 - chatRoomId: $chatRoomId, appVersion: $appVersion, category: ${request?.category}" }

        val strategy = strategyResolver.resolveStrategy(appVersion)
        val response = strategy.recommendQuestion(chatRoomId, requester, request ?: QuestionRecommendRequest())

        // 응답 타입에 따라 WebSocket 메시지 전송 및 HTTP 응답 처리
        return when (val body = response.body) {
            is QuestionRecommendResponseV2 -> {
                if (body.success && body.chat != null) {
                    sendQuestionWebSocketMessages(chatRoomId, requester, body.chat)
                }
                // Map으로 직접 응답 구성 (SavedChatDto.partner 직렬화 시 LazyInitializationException 방지)
                ResponseEntity.ok(mapOf(
                    "success" to body.success,
                    "question" to body.question,
                    "chat" to body.chat?.chatResponse,
                    "exhaustedMessage" to body.exhaustedMessage
                ))
            }
            is QuestionSendResult -> {
                sendQuestionWebSocketMessages(chatRoomId, requester, body)
                ResponseEntity.ok(body.chatResponse)
            }
            else -> response
        }
    }

    private fun sendQuestionWebSocketMessages(chatRoomId: Long, requester: Member, result: QuestionSendResult) {
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", result.chatResponse)
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
            result.requesterChatRoomResponse
        )
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${result.partner.getIdOrThrow()}",
            result.partnerChatRoomResponse
        )
    }

    private fun sendQuestionWebSocketMessages(chatRoomId: Long, requester: Member, chat: SavedChatDto) {
        messagingTemplate.convertAndSend("/sub/v1/chatroom/$chatRoomId", chat.chatResponse)
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${requester.getIdOrThrow()}",
            chat.requesterChatRoomResponse
        )
        messagingTemplate.convertAndSend(
            "/sub/v1/chatroom/member/${chat.partner.getIdOrThrow()}",
            chat.partnerChatRoomResponse
        )
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
