package codel.chat.business

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatSenderType
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.request.ChatSendRequest
import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.SavedChatDto
import codel.chat.repository.ChatRepository
import codel.chat.repository.ChatRoomRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.infrastructure.SignalJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
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
    private val chatJpaRepository: ChatJpaRepository
) {


    fun createInitialChatRoom(
        approver: Member,
        sender: Member,
        responseOfApproverQuestion : String
    ) : ChatRoomResponse{
        // 1. 채팅방 생성
        val newChatRoom = ChatRoom()
        val savedChatRoom = chatRoomJpaRepository.save(newChatRoom)

        // 2. 멤버 등록
        val approverMember = ChatRoomMember(chatRoom = savedChatRoom, member = approver)
        val senderMember = ChatRoomMember(chatRoom = savedChatRoom, member = sender)
        val savedApprover = chatRoomMemberJpaRepository.save(approverMember)
        val savedSender = chatRoomMemberJpaRepository.save(senderMember)

        // 3. 메시지 생성
        saveSystemMessages(savedChatRoom, savedApprover)
        saveUserMessages(savedChatRoom, savedApprover, senderMember, approver, responseOfApproverQuestion)

        return ChatRoomResponse.toResponse(newChatRoom, approver, 0,sender,0)
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
                chatContentType = ChatContentType.CODE_MATCHED
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
                chatContentType = ChatContentType.CODE_ONBOARDING
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
                message = profile.question,
                sentAt = now,
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.CODE_QUESTION
            ),
            Chat(
                chatRoom = chatRoom,
                fromChatRoomMember = fromApprover,
                message = profile.answer,
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


    fun createChatRoom(
        requester: Member,
        request: CreateChatRoomRequest,
    ): ChatRoomResponse {
        val partner = memberRepository.findDoneMember(request.partnerId)
        val savedChatRoom = chatRoomRepository.saveChatRoom(requester, partner)

        return ChatRoomResponse.toResponse(savedChatRoom, requester, 0, partner, 0)
    }

    @Transactional(readOnly = true)
    fun getChatRooms(
        requester: Member,
        pageable: Pageable,
    ): Page<ChatRoomResponse> {
        val pagedChatRooms = chatRoomRepository.findChatRooms(requester, pageable)

        return pagedChatRooms.map { chatRoom ->
            ChatRoomResponse.toResponse(
                chatRoom,
                requester,
                chatRoomMemberJpaRepository.findByChatRoomIdAndMember(chatRoom.getIdOrThrow(), requester)?.lastReadChat?.getIdOrThrow(),
                chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), requester),
                chatRepository.getUnReadMessageCount(chatRoom, requester),
            )
        }
    }

    fun saveChat(
        chatRoomId: Long,
        requester: Member,
        chatSendRequest: ChatSendRequest,
    ): SavedChatDto {
        val now = LocalDate.now()
        val recentChatTime = chatSendRequest.recentChatTime

        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)
        if(now != recentChatTime.toLocalDate()) {
            val dateMessage = now.toString()
            chatRepository.saveDateChat(chatRoom, dateMessage)
        }
        val savedChat = chatRepository.saveChat(chatRoomId, requester, chatSendRequest)

        chatRoom.updateRecentChat(savedChat)

        val partner = chatRoomRepository.findPartner(chatRoomId, requester)
        val unReadMessageCount = chatRepository.getUnReadMessageCount(chatRoom, requester)

        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        val chatRoomResponse = ChatRoomResponse.toResponse(chatRoom, requester, savedChat.getIdOrThrow(), partner, unReadMessageCount)

        return SavedChatDto(partner, chatRoomResponse, chatResponse)
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

    fun updateLastChat(
        chatRoomId: Long,
        lastReadChatId : Long,
        requester: Member,
    ) {
        val lastChat = chatRepository.findChat(lastReadChatId)

        chatRepository.upsertLastChat(chatRoomId, requester, lastChat)
    }

    fun updateUnlockChatRoom(requester: Member, chatRoomId: Long) : SavedChatDto{
        val chatRoom = chatRoomRepository.findChatRoomById(chatRoomId)

        chatRoom.unlock(requester.getIdOrThrow())

        val savedChat = chatJpaRepository.save(
            Chat.createSystemMessage(
                chatRoom = chatRoom,
                message = "코드해제 요청이 왔습니다.",
                chatContentType = ChatContentType.CODE_UNLOCKED_REQUEST
            )
        )

        val findPartner = chatRoomRepository.findPartner(chatRoom.getIdOrThrow(), requester)
        val chatResponse = ChatResponse.toResponse(requester, savedChat)
        val chatRoomResponse = ChatRoomResponse.toResponse(
            chatRoom, requester,
            chatRoomMemberJpaRepository.findByChatRoomIdAndMember(
                chatRoom.getIdOrThrow(),
                requester
            )?.lastReadChat?.getIdOrThrow(),
            findPartner,
            chatRepository.getUnReadMessageCount(chatRoom, requester)
        )

        return SavedChatDto(findPartner, chatRoomResponse, chatResponse)
    }
}

