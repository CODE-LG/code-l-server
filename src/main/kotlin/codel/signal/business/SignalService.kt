package codel.signal.business

import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import codel.signal.exception.SignalException
import codel.signal.infrastructure.SignalRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class SignalService(
    private val memberRepository: MemberRepository,
    private val signalRepository: SignalRepository
) {
    @Transactional
    fun sendSignal(fromMember: Member, toMemberId: Long): Signal {
        validateNotSelf(fromMember, toMemberId)
        val toMember = memberRepository.findMember(toMemberId)
        val lastSignal = signalRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)
        lastSignal?.validateSendable()
        val signal = Signal(fromMember = fromMember, toMember = toMember)
        return signalRepository.save(signal)
    }

    private fun validateNotSelf(fromMember: Member, toMemberId: Long) {
        if (fromMember.id == toMemberId) {
            throw SignalException(HttpStatus.BAD_REQUEST, "자기 자신에게는 시그널을 보낼 수 없습니다.")
        }
    }
} 