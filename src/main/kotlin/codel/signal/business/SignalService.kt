package codel.signal.business

import codel.chat.business.ChatService
import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatRoomMember
import codel.chat.domain.ChatRoomStatus
import codel.chat.domain.ChatSenderType
import codel.chat.infrastructure.ChatJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.repository.ChatRepository
import codel.config.Loggable
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.presentation.response.UnlockedMemberProfileResponse
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
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
    private val chatService: ChatService,
    private val notificationService: NotificationService
) : Loggable {
    @Transactional
    fun sendSignal(fromMember: Member, toMemberId: Long, message: String): Signal {
        validateNotSelf(fromMember.getIdOrThrow(), toMemberId)
        val toMember = memberRepository.findMember(toMemberId)
        val lastSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)
        lastSignal?.validateSendable()

        val signal = Signal(fromMember = fromMember, toMember = toMember, message = message)
        val savedSignal = signalJpaRepository.save(signal)
        
        // 알림 전송
        sendSignalNotification(toMember, fromMember)
        
        return savedSignal
    }
    
    private fun sendSignalNotification(receiver: Member, sender: Member) {
        receiver.fcmToken?.let { token ->
            val notification = Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = "새로운 시그널이 도착했어요 🔔",
                body = "${sender.getProfileOrThrow().getCodeNameOrThrow()}님이 시그널을 보냈습니다."
            )
            
            val startTime = System.currentTimeMillis()
            try {
                notificationService.send(notification)
                val duration = System.currentTimeMillis() - startTime
                
                // 성능 모니터링
                when {
                    duration > 1000 -> log.warn { "🐌 알림 전송 매우 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 발신자: ${sender.getIdOrThrow()}" }
                    duration > 500 -> log.warn { "⚠️ 알림 전송 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 발신자: ${sender.getIdOrThrow()}" }
                    else -> log.info { "✅ 시그널 알림 전송 성공 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 발신자: ${sender.getIdOrThrow()}" }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                log.warn(e) { "❌ 시그널 알림 전송 실패 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 발신자: ${sender.getIdOrThrow()}" }
            }
        } ?: run {
            log.info { "ℹ️ FCM 토큰이 없어 알림을 전송하지 않음 - 수신자: ${receiver.getIdOrThrow()}" }
        }
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
    ) : ChatRoomResponse{
        val findSignal = signalJpaRepository.findById(id)
            .orElseThrow { SignalException(HttpStatus.NOT_FOUND, "해당 시그널을 찾을 수 없습니다.") }

        validateMySignal(findSignal, me)
        findSignal.accept()

        val approvedSignal = signalJpaRepository.save(findSignal)

        val partner = findSignal.fromMember
        
        val chatRoomResponse = chatService.createInitialChatRoom(me, partner, approvedSignal.message)
        
        // 매칭 성공 알림 전송 (양쪽 모두에게)
        sendMatchingSuccessNotification(me, partner)

        return chatRoomResponse
    }
    
    private fun sendMatchingSuccessNotification(accepter: Member, sender: Member) {
        // 승인자(수신자)에게 알림
        sendMatchingNotification(accepter, sender)
        
        // 발신자에게 알림
        sendMatchingNotification(sender, accepter)
    }
    
    private fun sendMatchingNotification(receiver: Member, partner: Member) {
        receiver.fcmToken?.let { token ->
            val notification = Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = "코드매칭 성공! 채팅방이 열렸어요! 🎉",
                body = "${partner.getProfileOrThrow().getCodeNameOrThrow()}님과 대화를 시작해보세요!"
            )
            
            val startTime = System.currentTimeMillis()
            try {
                notificationService.send(notification)
                val duration = System.currentTimeMillis() - startTime
                
                // 성능 모니터링
                when {
                    duration > 1000 -> log.warn { "🐌 매칭 알림 전송 매우 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 상대방: ${partner.getIdOrThrow()}" }
                    duration > 500 -> log.warn { "⚠️ 매칭 알림 전송 느림 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 상대방: ${partner.getIdOrThrow()}" }
                    else -> log.info { "✅ 매칭 알림 전송 성공 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 상대방: ${partner.getIdOrThrow()}" }
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                log.warn(e) { "❌ 매칭 알림 전송 실패 (${duration}ms) - 수신자: ${receiver.getIdOrThrow()}, 상대방: ${partner.getIdOrThrow()}" }
            }
        } ?: run {
            log.info { "ℹ️ FCM 토큰이 없어 매칭 알림을 전송하지 않음 - 수신자: ${receiver.getIdOrThrow()}" }
        }
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
        return chatRoomMembers.map { chatRoomMember -> UnlockedMemberProfileResponse.toResponse(chatRoomMember.member, chatRoomMember.chatRoom.getUnlockedUpdateAtOrThrow()) }
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