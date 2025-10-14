package codel.chat.business

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatRoomStatus
import codel.chat.domain.ChatSenderType
import codel.chat.exception.ChatException
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatSendRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.SavedChatDto
import codel.chat.presentation.response.QuestionSendResult
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.config.Loggable
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.infrastructure.MemberJpaRepository
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.signal.infrastructure.SignalJpaRepository
import codel.question.business.QuestionService
import codel.question.domain.Question
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Transactional
@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRepository: ChatRepository,
    private val memberRepository: MemberRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val signalJpaRepository: SignalJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatJpaRepository: ChatJpaRepository,
    private val questionService: QuestionService,
    private val codeUnlockService: CodeUnlockService,
    private val memberJpaRepository: MemberJpaRepository,
    private val notificationService: NotificationService
) : Loggable {


    fun createInitialChatRoom(
        approver: Member,
        sender: Member,
        responseOfApproverQuestion : String
    ) : ChatRoomResponse{
        // 1. 채팅방 생성
        val managedApprover = memberRepository.findMemberWithProfileAndQuestion(
            approver.getIdOrThrow()
        ) ?: throw ChatException(HttpStatus.NOT_FOUND, "approver를 찾을 수 없습니다.")

        val newChatRoom = ChatRoom()
        val savedChatRoom = chatRoomJpaRepository.save(newChatRoom)

        // 2. 멤버 등록
        val approverMember = ChatRoomMember(chatRoom = savedChatRoom, member = managedApprover)
        val senderMember = ChatRoomMember(chatRoom = savedChatRoom, member = sender)
        val savedApprover = chatRoomMemberJpaRepository.save(approverMember)
        val savedSender = chatRoomMemberJpaRepository.save(senderMember)

        // 3. 메시지 생성
        saveSystemMessages(savedChatRoom, savedApprover)
        saveUserMessages(savedChatRoom, savedApprover, senderMember, managedApprover, responseOfApproverQuestion)

        // 4. 생성된 채팅방의 읽지 않은 메시지 수 계산 (시그널 발송자 기준)
        val senderUnReadCount = chatRepository.getUnReadMessageCount(savedChatRoom, sender)

        return ChatRoomResponse.toResponse(newChatRoom, sender, null, managedApprover, senderUnReadCount)
    }

    private fun saveSystemMessages(chatRoom: ChatRoom, from: ChatRoomMember) {
        val now = LocalDateTime.now()
        val today = LocalDate.now()

        val systemMessages = listOf(
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = from,
                message = "코드 매칭에 성공했어요!",
                sentAt = now,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.MATCHED
            ),
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = from,
                message = "✨ 코드 대화가 시작되었습니다.\n" +
                        "이어서 질문에 답하며 대화를 시작해보세요!\n\n" +
                        "\uD83D\uDD13 프로필 해제 안내\n" +
                        "상대의 숨겨진 프로필이 궁금하다면?\n[        ] 버튼을 눌러 상대의 숨겨진 히든 코드프로필 해제를 요청할 수 있어요.\n\n" +
                        "❓ 혹시 아직 어색한가요?\n위에 있는 [        ] 버튼을 확인해보세요.\n" +
                        "두 분의 공통 관심사에 맞춘 질문을 CODE가 추천해드립니다.\n\n ✨ 인연의 시작, CODE가 함께할게요.",
                sentAt = now,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.ONBOARDING
            ),
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = from,
                message = today.toString(), // 또는 한국어 포맷으로
                sentAt = now,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.TIME
            )
        )

        chatJpaRepository.saveAll(systemMessages)
    }

    private fun saveUserMessages(
        chatRoom: ChatRoom,
        fromApprover: ChatRoomMember,
        fromSender: ChatRoomMember,
        approver: Member,
        responseOfApproverQuestion : String
    ) {
        val now = LocalDateTime.now()
        val profile = approver.getProfileOrThrow()

        val userMessages = listOf(
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = fromApprover,
                message = profile.getRepresentativeQuestionOrThrow().content,
                sentAt = now,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.QUESTION
            ),
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = fromApprover,
                message = profile.getRepresentativeAnswerOrThrow(),
                sentAt = now,
                senderType = ChatSenderType.USER,
                chatContentType = ChatContentType.TEXT
            ),
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = fromSender,
                message = responseOfApproverQuestion,
                sentAt = now,
                senderType = ChatSenderType.USER,
                chatContentType = ChatContentType.TEXT
            )
        )

        val savedMessages = chatJpaRepository.saveAll(userMessages)
        chatRoom.updateRecentChat(savedMessages.last())
    }


    @Transactional(readOnly = true)
    fun getChatRooms(
        requester: Member,
        pageable: Pageable,
    ): Page<ChatRoomResponse> {
        // 활성 상태인 채팅방만 조회
        val activeChatRooms = chatRoomRepository.findActiveChatRoomsByMember(requester, pageable)

        return activeChatRooms.map { chatRoomInfo ->
            // 상대방 상태에 따른 읽지 않은 메시지 수 계산
            val unReadCount = calculateUnreadCount(
                chatRoomInfo.chatRoom, 
                requester, 
                chatRoomInfo.partnerChatRoomMember
            )
            
            // unlockInfo 추가
            val unlockInfo = codeUnlockService.getUnlockInfo(chatRoomInfo.chatRoom, requester)
            
            ChatRoomResponse.toResponseWithUnlockInfo(
                chatRoom = chatRoomInfo.chatRoom,
                requester = requester,
                lastReadChatId = chatRoomInfo.requesterChatRoomMember.lastReadChat?.getIdOrThrow(),
                partner = chatRoomInfo.partner,
                unReadMessageCount = unReadCount,
                unlockInfo = unlockInfo
            )
        }
    }

    /**
     * 상대방 상태에 따른 읽지 않은 메시지 수 계산
     */
    private fun calculateUnreadCount(
        chatRoom: ChatRoom, 
        requester: Member, 
        partnerChatRoomMember: ChatRoomMember?
    ): Int {
        return if (partnerChatRoomMember?.hasLeft() == true) {
            0 // 차단된 경우만 0
        } else {
            // 파트너가 나간 경우에도 일반적인 읽지 않은 메시지 수와 동일
            // 왜냐하면 파트너가 나간 이후로는 새 메시지가 없기 때문
            chatRepository.getUnReadMessageCount(chatRoom, requester)
        }
    }

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatSendRequest: ChatSendRequest,
    ): SavedChatDto {


        // 메시지 전송 가능 여부 확인
        validateCanSendMessage(chatRoomId, requester)
        
        val now = LocalDate.now()
        val recentChatTime = chatSendRequest.recentChatTime

        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        if(now != recentChatTime) {
            val dateMessage = now.toString()
            chatRepository.saveDateChat(chatRoom, dateMessage)
        }
        val savedChat = chatRepository.saveChat(chatRoomId, requester, chatSendRequest)

        chatRoom.updateRecentChat(savedChat)

        val partner = chatRoomRepository.findPartner(chatRoomId, requester)

        val unlockInfoOfRequester = codeUnlockService.getUnlockInfo(chatRoom, requester)
        val unlockInfoOfPartner = codeUnlockService.getUnlockInfo(chatRoom, partner)


        // 발송자와 수신자의 읽지 않은 메시지 수를 각각 계산
        val requesterUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, requester)
        val partnerUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, partner)

        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        
        // 발송자용 채팅방 응답 (본인 기준 읽지 않은 수)
        val requesterChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom, requester, savedChat.getIdOrThrow(), requester,
            requesterUnReadCount,
            unlockInfoOfRequester
        )
        
        // 수신자용 채팅방 응답 (상대방 기준 읽지 않은 수 - 새 메시지로 인해 증가)
        val partnerChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom, partner, null, requester,
            partnerUnReadCount,
            unlockInfoOfPartner
        )

        return SavedChatDto(partner, requesterChatRoomResponse, partnerChatRoomResponse, chatResponse)
    }

    @Transactional(readOnly = true)
    fun getChats(
        chatRoomId: Long,
        lastChatId : Long?,
        requester: Member,
        pageable: Pageable,
    ): Page<ChatResponse> {
        val pagedChats = chatRepository.findNextChats(chatRoomId, lastChatId, pageable)
        return pagedChats.map { chat -> ChatResponse.toResponse(requester, chat) }
    }

    @Transactional(readOnly = true)
    fun getPreviousChats(
        chatRoomId: Long,
        lastChatId : Long?,
        requester: Member,
        pageable: Pageable,
    ): Page<ChatResponse> {
        val pagedChats = chatRepository.findPrevChats(chatRoomId, lastChatId, pageable)
        return pagedChats.map { chat -> ChatResponse.toResponse(requester, chat) }
    }

    fun updateLastChat(
        chatRoomId: Long,
        lastReadChatId : Long,
        requester: Member,
    ) {
        val lastChat = chatRepository.findChat(lastReadChatId)
        chatRepository.upsertLastChat(chatRoomId, requester, lastChat)
        
        // 읽음 처리 후 상대방에게도 업데이트된 정보 전송 (읽지 않은 수 감소 반영)
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        val partner = chatRoomRepository.findPartner(chatRoomId, requester)
        val partnerUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, partner)

        val updatedChatRoomResponse = ChatRoomResponse.toResponse(
            chatRoom, partner, 
            chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, partner)?.lastReadChat?.getIdOrThrow(),
            requester, 
            partnerUnReadCount
        )
        
        //TODO WebSocket으로 상대방에게 업데이트 전송 (Spring Event 등을 활용할 수도 있음)
        // 이 부분은 Controller나 별도 이벤트 핸들러에서 처리하는 것이 좋습니다.
    }

    fun updateUnlockChatRoom(requester: Member, chatRoomId: Long) : SavedChatDto{
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)

        val savedChat = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = chatRoom,
                message = "코드해제 요청이 왔습니다.",
                chatContentType = ChatContentType.UNLOCKED_REQUEST
            )
        )

        val partner = chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), requester)
        
        // 코드 해제 요청 알림 전송
        sendCodeUnlockNotification(partner, requester)
        
        // 발송자와 수신자의 읽지 않은 메시지 수를 각각 계산
        val requesterUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, requester)
        val partnerUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, partner)
        
        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        val unlockInfoOfRequester = codeUnlockService.getUnlockInfo(chatRoom, requester)
        val unlockInfoOfPartner = codeUnlockService.getUnlockInfo(chatRoom, partner)

        // 발송자용 채팅방 응답
        val requesterChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom, requester,
            chatRoomMemberJpaRepository.findByChatRoomIdAndMember(
                chatRoom.getIdOrThrow(),
                requester
            )?.lastReadChat?.getIdOrThrow(),
            partner,
            requesterUnReadCount,
            unlockInfoOfRequester
        )
        
        // 수신자용 채팅방 응답 (읽지 않은 수 증가)
        val partnerChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom, partner, null, requester, 
            partnerUnReadCount,
            unlockInfoOfPartner
        )

        return SavedChatDto(partner, requesterChatRoomResponse, partnerChatRoomResponse, chatResponse)
    }
    
    private fun sendCodeUnlockNotification(receiver: Member, requester: Member) {
        receiver.fcmToken?.let { token ->
            val notification = Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = "${requester.getProfileOrThrow().getCodeNameOrThrow()}님이 코드 해제를 요청했어요 🔐",
                body = "상대방의 프로필을 확인해보세요!"
            )
            
            val startTime = System.currentTimeMillis()
            try {
                notificationService.send(notification)
                val duration = System.currentTimeMillis() - startTime
                
                when {
                    duration > 1000 -> log.warn { "🐌 코드 해제 요청 알림 전송 매우 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 요청자: ${requester.getIdOrThrow()}" }
                    duration > 500 -> log.warn { "⚠️ 코드 해제 요청 알림 전송 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 요청자: ${requester.getIdOrThrow()}" }
                    else -> log.info { "✅ 코드 해제 요청 알림 전송 성공 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 요청자: ${requester.getIdOrThrow()}" }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                log.warn(e) { "❌ 코드 해제 요청 알림 전송 실패 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 요청자: ${requester.getIdOrThrow()}" }
            }
        } ?: run {
            log.info { "ℹ️ FCM 토큰이 없어 코드 해제 요청 알림을 전송하지 않음 - 수신자: ${receiver.getIdOrThrow()}" }
        }
    }

    /**
     * 랜덤 질문을 채팅방에 전송
     */
    fun sendRandomQuestion(chatRoomId: Long, requester: Member): QuestionSendResult {
        // 1. 채팅방 검증 (채팅 도메인 책임)
        val chatRoom = chatRoomJpaRepository.findById(chatRoomId)
            .orElseThrow { ChatException(HttpStatus.NOT_FOUND, "채팅방을 찾을 수 없습니다.") }
        
        validateChatRoomMember(chatRoomId, requester)
        val partner = findPartner(chatRoomId, requester)
        
        // 2. 질문 선택 (질문 도메인에 위임)
        val availableQuestions = questionService.findUnusedQuestionsByChatRoom(chatRoomId)
        if (availableQuestions.isEmpty()) {
            throw ChatException(HttpStatus.NO_CONTENT, "더 이상 사용할 수 있는 질문이 없습니다.")
        }
        val selectedQuestion = questionService.selectRandomQuestion(availableQuestions)
        
        // 3. 질문 사용 표시 (질문 도메인에 위임)
        questionService.markQuestionAsUsed(chatRoomId, selectedQuestion, requester)
        codeUnlockService.getUnlockInfo(chatRoom, requester)
        
        // 4. 채팅 메시지 생성 (채팅 도메인 책임)
        val savedChat = createQuestionSystemMessage(chatRoom, selectedQuestion, requester)
        chatRoom.updateRecentChat(savedChat)
        
        return buildQuestionSendResult(requester, partner, savedChat)
    }

    /**
     * 채팅방 멤버 권한 검증
     */
    private fun validateChatRoomMember(chatRoomId: Long, member: Member) {
        chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, member)
            ?: throw ChatException(HttpStatus.FORBIDDEN, "해당 채팅방에 접근할 권한이 없습니다.")
    }

    /**
     * 질문 시스템 메시지 생성
     */
    private fun createQuestionSystemMessage(
        chatRoom: ChatRoom,
        question: Question,
        requester: Member
    ): Chat {
        val message = "💭 ${question.content}\n\n_${requester.getProfileOrThrow().codeName}님이 질문을 추천했습니다._"

        // 요청자의 ChatRoomMember 찾기
        val requesterChatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "채팅방 멤버를 찾을 수 없습니다.")
        
        val systemMessage = Chat(
            chatRoom = chatRoom,
            fromChatRoomMember = requesterChatRoomMember, // null 대신 실제 멤버 할당
            message = message,
            senderType = ChatSenderType.SYSTEM,
            chatContentType = ChatContentType.QUESTION,
            sentAt = LocalDateTime.now()
        )
        
        return chatJpaRepository.save(systemMessage)
    }

    /**
     * 질문 전송 결과 구성
     */
    private fun buildQuestionSendResult(requester: Member, partner: Member, savedChat: Chat): QuestionSendResult {
        val chatRoom = savedChat.chatRoom

        // 각자의 읽지 않은 메시지 수 계산
        val requesterUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, requester)
        val partnerUnReadCount = chatRepository.getUnReadMessageCount(chatRoom, partner)
        val unlockInfoRequester = codeUnlockService.getUnlockInfo(chatRoom, requester)
        val unlockInfoPartner = codeUnlockService.getUnlockInfo(chatRoom, partner)

        // 발송자용 채팅방 응답
        val requesterChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom,
            requester,
            savedChat.getIdOrThrow(),
            partner,
            requesterUnReadCount,
            unlockInfoRequester
        )
        
        // 수신자용 채팅방 응답 (읽지 않은 수 증가)
        val partnerChatRoomResponse = ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom,
            partner,
            null, // 상대방은 아직 읽지 않았으므로 null
            requester,
            partnerUnReadCount,
            unlockInfoPartner
        )

        return QuestionSendResult(
            chatResponse = ChatResponse.toResponse(requester, savedChat),
            partner = partner,
            requesterChatRoomResponse = requesterChatRoomResponse,
            partnerChatRoomResponse = partnerChatRoomResponse
        )
    }

    /**
     * 채팅방 나가기
     */
    fun leaveChatRoom(chatRoomId: Long, member: Member) {
        val chatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, member)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방의 멤버가 아닙니다.")

        // 이미 나간 상태인지 확인
        if (chatRoomMember.hasLeft()) {
            throw ChatException(HttpStatus.BAD_REQUEST, "이미 나간 채팅방입니다.")
        }

        // 개별 사용자 상태 변경
        chatRoomMember.leave()
    }

    fun closeConversation(chatRoomId: Long, requester: Member) {
        val chatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoomId, requester)
            ?: throw ChatException(HttpStatus.BAD_REQUEST, "해당 채팅방의 멤버가 아닙니다.")

        // 이미 나간 상태인지 확인
        if (chatRoomMember.hasLeft()) {
            throw ChatException(HttpStatus.BAD_REQUEST, "이미 나간 채팅방입니다.")
        }
        chatRoomMember.closeConversation()

        // 시스템 메시지 추가
        val closeConversationMessage = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = chatRoomMember.chatRoom,
                message = "${requester.getProfileOrThrow().codeName}님이 대화를 종료하였습니다.",
                chatContentType = ChatContentType.CLOSE_CONVERSATION
            )
        )

        // 최근 채팅 업데이트
        chatRoomMember.chatRoom.updateRecentChat(closeConversationMessage)
    }

    /**
     * 메시지 전송 가능 여부 확인
     */
    fun validateCanSendMessage(chatRoomId: Long, sender: Member) {
        if (!chatRepository.canSendMessage(chatRoomId, sender)) {
            throw ChatException(HttpStatus.FORBIDDEN, "메시지를 전송할 수 없습니다. 채팅방 상태를 확인해주세요.")
        }
    }

    /**
     * 채팅방의 상대방 찾기 (공개 메서드)
     */
    fun findPartner(chatRoomId: Long, requester: Member): Member {
        return chatRoomRepository.findPartner(chatRoomId, requester)
    }

    /**
     * 채팅방 ID로 채팅방 조회 (2단계에서 추가)
     */
    fun findChatRoomById(chatRoomId: Long): ChatRoom {
        return chatRoomRepository.findChatRoomById(chatRoomId)
    }

    /**
     * ChatResponse 생성 헬퍼 (2단계에서 추가)
     */
    fun buildChatResponse(requester: Member, chat: Chat): ChatResponse {
        return ChatResponse.toResponse(requester, chat)
    }

    /**
     * ChatRoomResponse 생성 헬퍼 (2단계에서 추가)
     */
    fun buildChatRoomResponse(chatRoom: ChatRoom, requester: Member, partner: Member): ChatRoomResponse {
        val requesterChatRoomMember = chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)
        val unReadCount = chatRepository.getUnReadMessageCount(chatRoom, requester)
        val unlockInfo = codeUnlockService.getUnlockInfo(chatRoom, requester)

        return ChatRoomResponse.toResponseWithUnlockInfo(
            chatRoom = chatRoom,
            requester = requester,
            lastReadChatId = requesterChatRoomMember?.lastReadChat?.getIdOrThrow(),
            partner = partner,
            unReadMessageCount = unReadCount,
            unlockInfo = unlockInfo
        )
    }
}

