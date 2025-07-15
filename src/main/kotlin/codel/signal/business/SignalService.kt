package codel.signal.business

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
) {
    @Transactional
    fun sendSignal(fromMember: Member, toMemberId: Long): Signal {
        validateNotSelf(fromMember, toMemberId)
        val toMember = memberRepository.findMember(toMemberId)
        val lastSignal = signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)
        lastSignal?.validateSendable()
        val signal = Signal(fromMember = fromMember, toMember = toMember)
        return signalJpaRepository.save(signal)
    }

    private fun validateNotSelf(fromMember: Member, toMemberId: Long) {
        if (fromMember.id == toMemberId) {
            throw SignalException(HttpStatus.BAD_REQUEST, "자기 자신에게는 시그널을 보낼 수 없습니다.")
        }
    }

    @Transactional(readOnly = true)
    fun getReceivedSignals(
        me: Member,
        page: Int,
        size: Int
    ): Page<Signal>{
        val pageable = PageRequest.of(page, size)
        val receivedSignals = signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)
        return PageImpl(receivedSignals, pageable, receivedSignals.size.toLong())
    }


    @Transactional(readOnly = true)
    fun getSendSignalByMe(
        me: Member,
        page: Int,
        size: Int) : Page<Signal>{
        val pageable = PageRequest.of(page, size)
        val sendSignals = signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)
        return PageImpl(sendSignals, pageable, sendSignals.size.toLong())
    }


    @Transactional
    fun acceptSignal(
        me: Member,
        id: Long) {
        val findSignal = signalJpaRepository.findById(id).get()
        if(findSignal.toMember.id != me.id){
            throw SignalException(HttpStatus.BAD_REQUEST, "내게 온 시그널만 수락할 수 있어요.")
        }
        findSignal.validateChangeAcceptable()
        findSignal.accepct()
        signalJpaRepository.save(findSignal)
    }

}