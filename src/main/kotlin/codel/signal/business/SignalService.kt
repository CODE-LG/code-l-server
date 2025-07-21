package codel.signal.business

import codel.chat.domain.ChatRoomStatus
import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
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

@Service
class SignalService(
    private val memberRepository: MemberRepository,
    private val signalJpaRepository: SignalJpaRepository,
    private val chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository,
) {
    @Transactional
    fun sendSignal(fromMember: Member, toMemberId: Long): Signal {
        val toMember = memberRepository.findMember(toMemberId)
        val lastSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)
            ?: throw SignalException(HttpStatus.BAD_REQUEST, "시그널 정보를 찾을 수 없습니다.")

        lastSignal.validateSendable(fromMember.getIdOrThrow(), toMemberId)
        val signal = Signal(fromMember = fromMember, toMember = toMember)
        return signalJpaRepository.save(signal)
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
        signalJpaRepository.save(findSignal)
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
    fun getUnlockedSignal(member: Member, page: Int, size: Int): Page<Member> {
        val pageable = PageRequest.of(page, size)

        val chatRoomMembers =
            chatRoomMemberJpaRepository.findUnlockedOpponentsWithProfile(member, ChatRoomStatus.UNLOCKED, pageable)
        // 챗룸멤버를 멤버로 찾아온다.
        return chatRoomMembers.map { chatRoomMember -> chatRoomMember.member }
        // 챗룸멤버라는 리스트를 가져온 상태에서 챗룸의 정보를 가져온다.
        // 챗룸 상태가 코드해제된 방에 대해서 알아오고, 코드해제된 방 중 상대방에 대한 멤버 정보 + 프로필 정보를 함꼐 가져온다.
    }

    @Transactional
    fun hideSignal(me: Member, id: Long) {
        val findSignal = signalJpaRepository.findById(id)
            .orElseThrow { SignalException(HttpStatus.NOT_FOUND, "해당 시그널을 찾을 수 없습니다.") }

        findSignal.hide(me.getIdOrThrow())
    }
}