package codel.signal.business

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatRoomStatus
import codel.chat.domain.ChatSenderType
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.response.UnlockedMemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import codel.signal.exception.SignalException
import codel.signal.infrastructure.SignalJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class SignalService(
    private val memberRepository: MemberRepository,
    private val signalJpaRepository: SignalJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository,
    private val chatJpaRepository: ChatJpaRepository,
) {
    @Transactional
    fun sendSignal(fromMember: Member, toMemberId: Long, message: String): Signal {
        validateNotSelf(fromMember.getIdOrThrow(), toMemberId)
        val toMember = memberRepository.findMember(toMemberId)
        val lastSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)
        lastSignal?.validateSendable()

        val signal = Signal(fromMember = fromMember, toMember = toMember, message = message)
        return signalJpaRepository.save(signal)
    }

    private fun validateNotSelf(fromMemberId: Long, toMemberId: Long) {
        if (fromMemberId == toMemberId) {
            throw SignalException(HttpStatus.BAD_REQUEST, "자기 자신에게는 시그널을 보낼 수 없습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getReceivedSignals(
        me: Member,
        page: Int,
        size: Int
    ): Page<Signal> {
        val pageable = PageRequest.of(page, size)
        val receivedSignals = signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)
        return PageImpl(receivedSignals, pageable, receivedSignals.size.toLong())
    }


    @Transactional(readOnly = true)
    fun getSendSignalByMe(
        me: Member,
        page: Int,
        size: Int
    ): Page<Signal> {
        val pageable = PageRequest.of(page, size)
        val sendSignals = signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)
        return PageImpl(sendSignals, pageable, sendSignals.size.toLong())
    }


    @Transactional
    fun acceptSignal(
        me: Member,
        id: Long
    ) {
        val findSignal = signalJpaRepository.findById(id)
            .orElseThrow { SignalException(HttpStatus.NOT_FOUND, "해당 시그널을 찾을 수 없습니다.") }

        validateMySignal(findSignal, me)
        findSignal.accept()

        val approvedSignal = signalJpaRepository.save(findSignal)

        val partner = findSignal.fromMember
        val newChatRoom = ChatRoom()
        val savedChatRoom = chatRoomJpaRepository.save(newChatRoom)
        val chatRoomMemberForApproveMember = ChatRoomMember(chatRoom = savedChatRoom, member = me)
        val chatRoomMemberForSendMember = ChatRoomMember(chatRoom = savedChatRoom, member = partner)
        val savedChatRoomMemberByApprover = chatRoomMemberJpaRepository.save(chatRoomMemberForApproveMember)
        val savedChatRoomMemberBySender = chatRoomMemberJpaRepository.save(chatRoomMemberForSendMember)
        saveSystemMessage(savedChatRoom, savedChatRoomMemberByApprover)
        saveUserMessage(savedChatRoom, savedChatRoomMemberByApprover, me, savedChatRoomMemberBySender, approvedSignal)
    }

    private fun saveSystemMessage(
        savedChatRoom: ChatRoom,
        savedChatRoomMemberByApprover: ChatRoomMember
    ) {
        chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberByApprover,
                message = "코드 매칭에 성공했어요!",
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.CODE_MATCHED
            )
        )

        chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberByApprover,
                message = "✨ 코드 대화가 시작되었습니다.\n" +
                        "이어서 질문에 답하며 대화를 시작해보세요!\n" +
                        "\n" +
                        "\uD83D\uDD13 프로필 해제 안내\n" +
                        "상대의 숨겨진 프로필이 궁금하다면?\n" +
                        "[        ] 버튼을 눌러 상대의 숨겨진 히든 코드프로필 해제를 요청할 수 있어요.\n" +
                        "\n" +
                        "❓ 혹시 아직 어색한가요?\n" +
                        "위에 있는 [        ] 버튼을 확인해보세요.\n" +
                        "두 분의 공통 관심사에 맞춘 질문을 CODE가 추천해드립니다.\n" +
                        "\n" +
                        " ✨ 인연의 시작, CODE가 함께할게요.",
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.CODE_ONBOARDING
            )
        )

        chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberByApprover,
                message = LocalDate.now().toString(),
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.TIME
            )
        )



    }

    private fun saveUserMessage(
        savedChatRoom: ChatRoom,
        savedChatRoomMemberByApprover: ChatRoomMember,
        me: Member,
        savedChatRoomMemberBySender: ChatRoomMember,
        findSignal: Signal
    ) {
        chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberByApprover,
                message = me.getProfileOrThrow().question,
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.SYSTEM,
                chatContentType = ChatContentType.CODE_QUESTION
            )
        )
        chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberByApprover,
                message = me.getProfileOrThrow().answer,
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.USER,
                chatContentType = ChatContentType.TEXT
            )
        )
        val savedUserAnswer = chatJpaRepository.save(
            Chat(
                chatRoom = savedChatRoom,
                fromChatRoomMember = savedChatRoomMemberBySender,
                message = findSignal.message,
                sentAt = LocalDateTime.now(),
                senderType = ChatSenderType.USER,
                chatContentType = ChatContentType.TEXT
            )
        )
        savedChatRoom.updateRecentChat(savedUserAnswer)
    }

    private fun validateMySignal(findSignal: Signal, me: Member) {
        if (findSignal.toMember.id != me.id) {
            throw SignalException(HttpStatus.BAD_REQUEST, "내게 온 시그널만 수락할 수 있어요.")
        }
    }

    @Transactional
    fun rejectSignal(
        me: Member,
        id: Long
    ) {
        val findSignal = signalJpaRepository.findById(id)
            .orElseThrow { SignalException(HttpStatus.NOT_FOUND, "해당 시그널을 찾을 수 없습니다.") }

        validateMySignal(findSignal, me)
        findSignal.reject()
        signalJpaRepository.save(findSignal)
    }


    @Transactional(readOnly = true)
    fun getAcceptedSignals(
        me: Member,
        page: Int,
        size: Int
    ): Page<Signal> {
        val pageable = PageRequest.of(page, size)
        val acceptedSignals = signalJpaRepository.findByMemberAndStatus(me, SignalStatus.APPROVED)
        return PageImpl(acceptedSignals, pageable, acceptedSignals.size.toLong())
    }

    @Transactional(readOnly = true)
    fun getUnlockedSignal(member: Member, page: Int, size: Int): Page<UnlockedMemberProfileResponse> {
        val pageable = PageRequest.of(page, size)

        val chatRoomMembers =
            chatRoomMemberJpaRepository.findUnlockedOpponentsWithProfile(member, ChatRoomStatus.UNLOCKED, pageable)
        // 챗룸멤버를 멤버로 찾아온다.
        return chatRoomMembers.map { chatRoomMember -> UnlockedMemberProfileResponse.toResponse(chatRoomMember.member, chatRoomMember.chatRoom.getUnlockedUpdateAt()) }
        // 챗룸멤버라는 리스트를 가져온 상태에서 챗룸의 정보를 가져온다.
        // 챗룸 상태가 코드해제된 방에 대해서 알아오고, 코드해제된 방 중 상대방에 대한 멤버 정보 + 프로필 정보를 함꼐 가져온다.
    }

    @Transactional
    fun hideSignal(me: Member, id: Long) {
        val findSignal = signalJpaRepository.findById(id)
            .orElseThrow { SignalException(HttpStatus.NOT_FOUND, "해당 시그널을 찾을 수 없습니다.") }

        findSignal.hide(me.getIdOrThrow())
    }

    @Transactional
    fun hideSignals(me: Member, signalIds : List<Long>){
        signalIds.forEach { signalId ->
            val findSignal = signalJpaRepository.findById(signalId)
                .orElseThrow { SignalException(HttpStatus.BAD_REQUEST, "해당 시그널을 찾을 수 없습니다.") }

            findSignal.hide(me.getIdOrThrow())
        }
    }
}