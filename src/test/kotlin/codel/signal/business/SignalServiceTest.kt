package codel.signal.business

import codel.chat.infrastructure.ChatRoomMemberJpaRepository
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import codel.signal.domain.SignalTestHelper
import codel.signal.exception.SignalException
import codel.signal.infrastructure.SignalJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class SignalServiceTest {
    @Mock
    lateinit var memberRepository: MemberRepository

    @Mock
    lateinit var signalJpaRepository: SignalJpaRepository

    @Mock
    lateinit var chatRoomMemberJpaRepository: ChatRoomMemberJpaRepository

    @InjectMocks
    lateinit var signalService: SignalService

    @DisplayName("시그널 전송 시 정상적으로 저장된다")
    @Test
    fun sendSignal_success() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val toMemberId = 2L

        val message = "저는 이렇게 생각해요!"
        val savedSignal = Signal(fromMember = fromMember, toMember = toMember)

        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.save(any())).willReturn(savedSignal)


        // when
        val result = signalService.sendSignal(fromMember, toMemberId, message)

        // then
        assertThat(result.fromMember).isEqualTo(fromMember)
        assertThat(result.toMember).isEqualTo(toMember)
        assertThat(result.status).isEqualTo(SignalStatus.PENDING)
    }

    @DisplayName("자기 자신에게 시그널을 보내면 예외가 발생한다")
    @Test
    fun sendSignal_self_fail() {
        // given
        val fromMember = mock(Member::class.java)
        val toMemberId = 1L

        val message = "저는 이렇게 생각해요!"
        given(fromMember.id).willReturn(1L)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId, message)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("자기 자신에게는 시그널을 보낼 수 없습니다.")
    }

    @DisplayName("이미 PENDING 상태의 시그널이 있으면 예외가 발생한다")
    @Test
    fun sendSignal_pending_fail() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val toMemberId = 2L

        val message = "저는 이렇게 생각해요!"
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.PENDING)
        lastSignal.createdAt = LocalDateTime.now().minusDays(1)
        lastSignal.updatedAt = LocalDateTime.now().minusDays(1)
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(
            lastSignal
        )

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId, message)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("이미 시그널을 보낸 상대입니다.")
    }

    @DisplayName("이미 ACCEPTED 상태의 시그널이 있으면 예외가 발생한다")
    @Test
    fun sendSignal_accepted_fail() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val toMemberId = 2L

        val message = "저는 이렇게 생각해요!"
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.APPROVED)

        lastSignal.createdAt = LocalDateTime.now().minusDays(1)
        lastSignal.updatedAt = LocalDateTime.now().minusDays(1)
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(
            lastSignal
        )

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId, message)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("이미 시그널을 보낸 상대입니다.")
    }

    @DisplayName("REJECTED 상태지만 7일이 지나지 않았으면 예외가 발생한다")
    @Test
    fun sendSignal_rejected_within_7days_fail() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val message = "저는 이렇게 생각해요!"
        val toMemberId = 2L
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.REJECTED)

        lastSignal.createdAt = LocalDateTime.now().minusDays(8)
        lastSignal.updatedAt = LocalDateTime.now().minusDays(6)
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(
            lastSignal
        )

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId, message)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("거절된 상대에게는 7일 후에 다시 시그널을 보낼 수 있습니다.")
    }

    @DisplayName("REJECTED 상태지만 7일이 지났으면 시그널을 재전송할 수 있다")
    @Test
    fun sendSignal_success_after_7days() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val message = "저는 이렇게 생각해요!"
        val toMemberId = 2L
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.REJECTED)
        lastSignal.createdAt = LocalDateTime.now().minusDays(10)
        lastSignal.updatedAt = LocalDateTime.now().minusDays(8)
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(
            lastSignal
        )
        val savedSignal = Signal(fromMember = fromMember, toMember = toMember)
        given(signalJpaRepository.save(any())).willReturn(savedSignal)

        // when & then
        val sendSignal = signalService.sendSignal(fromMember, toMemberId, message)
        assertThat(sendSignal.status).isEqualTo(SignalStatus.PENDING)
    }

    @DisplayName("여러 상태의 받은 시그널 중 PENDING만 반환된다")
    @Test
    fun getReceivedSignals_onlyPending() {
        // given
        val me = mock(Member::class.java)
        val fromMember1 = mock(Member::class.java)
        val fromMember2 = mock(Member::class.java)
        val pendingSignal = Signal(fromMember = fromMember1, toMember = me, status = SignalStatus.PENDING)
        val approvedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.APPROVED)
        val rejectedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(pendingSignal))

        // when
        val result = signalService.getReceivedSignals(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(pendingSignal)
        assertThat(result.content).doesNotContain(approvedSignal, rejectedSignal)
    }

    @DisplayName("받은 시그널이 없을 때 빈 Page가 반환된다")
    @Test
    fun getReceivedSignals_empty() {
        // given
        val me = mock(Member::class.java)
        given(signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)).willReturn(emptyList())

        // when
        val result = signalService.getReceivedSignals(me, 0, 10)

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @DisplayName("toMember가 나(me)가 아닌 시그널은 결과에 포함되지 않는다")
    @Test
    fun getReceivedSignals_excludesOtherToMember() {
        // given
        val me = mock(Member::class.java)
        val notMe = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalForMe = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.PENDING)
        val signalForOther = Signal(fromMember = fromMember, toMember = notMe, status = SignalStatus.PENDING)
        given(signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(signalForMe))

        // when
        val result = signalService.getReceivedSignals(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(signalForMe)
        assertThat(result.content).doesNotContain(signalForOther)
    }

    @DisplayName("여러 상태의 전송 시그널 중 PENDING만 반환된다")
    @Test
    fun getSendSignal_onlyPending() {
        // given
        val me = mock(Member::class.java)
        val fromMember1 = mock(Member::class.java)
        val fromMember2 = mock(Member::class.java)

        val pendingSignal = Signal(fromMember = fromMember1, toMember = me, status = SignalStatus.PENDING)
        val approvedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.APPROVED)
        val rejectedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(pendingSignal))

        // when
        val result = signalService.getSendSignalByMe(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(pendingSignal)
        assertThat(result.content).doesNotContain(approvedSignal, rejectedSignal)
    }

    @DisplayName("전송 시그널이 없을 때 빈 Page가 반환된다")
    @Test
    fun getSendSignals_empty() {
        // given
        val me = mock(Member::class.java)
        given(signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)).willReturn(emptyList())

        // when
        val result = signalService.getSendSignalByMe(me, 0, 10)

        // then
        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @DisplayName("toMember가 나(me)가 아닌 시그널은 결과에 포함되지 않는다")
    @Test
    fun getSendSignals_excludesOtherToMember() {
        // given
        val me = mock(Member::class.java)
        val notMe = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalForMe = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.PENDING)
        val signalForOther = Signal(fromMember = fromMember, toMember = notMe, status = SignalStatus.PENDING)
        given(signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(signalForMe))

        // when
        val result = signalService.getSendSignalByMe(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(signalForMe)
        assertThat(result.content).doesNotContain(signalForOther)
    }

    @DisplayName("받은 시그널을 정상적으로 승인한다")
    @Test
    fun acceptSignal_success() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        val signal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.PENDING)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(signal))
        given(signalJpaRepository.save(signal)).willReturn(signal)

        // when
        signalService.acceptSignal(me, signalId)

        // then
        assertThat(signal.status).isEqualTo(SignalStatus.APPROVED)
    }

    @DisplayName("승인 할 때, 내게 온 시그널이 아니면 예외가 발생한다")
    @Test
    fun acceptSignal_notMySignal_fail() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val notMe = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        given(notMe.id).willReturn(3L)
        val signal = Signal(fromMember = fromMember, toMember = notMe, status = SignalStatus.PENDING)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(signal))

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.acceptSignal(me, signalId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("내게 온 시그널만 수락할 수 있어요.")
    }

    @DisplayName("이미 처리된 시그널(ACCEPTED/REJECTED)은 승인할 수 없다")
    @Test
    fun acceptSignal_alreadyProcessed_fail() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        val approvedSignal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.APPROVED)
        val rejectedSignal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(approvedSignal))

        // when & then
        val exception1 = assertThrows<SignalException> {
            signalService.acceptSignal(me, signalId)
        }
        assertThat(exception1.status).isEqualTo(HttpStatus.BAD_REQUEST)
        // 상태 메시지는 도메인 예외 메시지에 맞게 검증

        // REJECTED 상태도 검증
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(rejectedSignal))
        val exception2 = assertThrows<SignalException> {
            signalService.acceptSignal(me, signalId)
        }
        assertThat(exception2.status).isEqualTo(HttpStatus.BAD_REQUEST)
    }


    @DisplayName("받은 시그널을 정상적으로 거절한다")
    @Test
    fun rejectSignal_success() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        val signal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.PENDING)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(signal))
        given(signalJpaRepository.save(signal)).willReturn(signal)

        // when
        signalService.rejectSignal(me, signalId)

        // then
        assertThat(signal.status).isEqualTo(SignalStatus.REJECTED)
    }

    @DisplayName("거절 할 때, 내게 온 시그널이 아니면 예외가 발생한다")
    @Test
    fun rejectSignal_notMySignal_fail() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val notMe = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        given(notMe.id).willReturn(3L)
        val signal = Signal(fromMember = fromMember, toMember = notMe, status = SignalStatus.PENDING)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(signal))

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.rejectSignal(me, signalId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("내게 온 시그널만 수락할 수 있어요.")
    }

    @DisplayName("이미 처리된 시그널(ACCEPTED/REJECTED)은 거절할 수 없다")
    @Test
    fun rejectSignal_alreadyProcessed_fail() {
        // given
        val me = mock(Member::class.java)
        val fromMember = mock(Member::class.java)
        val signalId = 1L
        given(me.id).willReturn(2L)
        val approvedSignal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.APPROVED)
        val rejectedSignal = Signal(fromMember = fromMember, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(approvedSignal))

        // when & then
        val exception1 = assertThrows<SignalException> {
            signalService.rejectSignal(me, signalId)
        }
        assertThat(exception1.status).isEqualTo(HttpStatus.BAD_REQUEST)
        // 상태 메시지는 도메인 예외 메시지에 맞게 검증

        // REJECTED 상태도 검증
        given(signalJpaRepository.findById(signalId)).willReturn(Optional.of(rejectedSignal))
        val exception2 = assertThrows<SignalException> {
            signalService.rejectSignal(me, signalId)
        }
        assertThat(exception2.status).isEqualTo(HttpStatus.BAD_REQUEST)
    }
} 