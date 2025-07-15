package codel.signal.business

import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import codel.signal.infrastructure.SignalJpaRepository
import codel.signal.exception.SignalException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import codel.signal.domain.SignalTestHelper

@ExtendWith(MockitoExtension::class)
class SignalServiceTest {
    @Mock
    lateinit var memberRepository: MemberRepository
    @Mock
    lateinit var signalJpaRepository: SignalJpaRepository
    @InjectMocks
    lateinit var signalService: SignalService

    @DisplayName("시그널 전송 시 정상적으로 저장된다")
    @Test
    fun sendSignal_success() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val toMemberId = 2L
        val savedSignal = Signal(fromMember = fromMember, toMember = toMember)

        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.save(any())).willReturn(savedSignal)

        // when
        val result = signalService.sendSignal(fromMember, toMemberId)

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
        given(fromMember.id).willReturn(1L)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
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
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.PENDING)
        SignalTestHelper.setCreatedAt(lastSignal, LocalDateTime.now().minusDays(1))
        SignalTestHelper.setUpdatedAt(lastSignal, LocalDateTime.now().minusDays(1))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
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
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.ACCEPTED)
        SignalTestHelper.setCreatedAt(lastSignal, LocalDateTime.now().minusDays(1))
        SignalTestHelper.setUpdatedAt(lastSignal, LocalDateTime.now().minusDays(1))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
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
        val toMemberId = 2L
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.REJECTED)
        SignalTestHelper.setCreatedAt(lastSignal, LocalDateTime.now().minusDays(8))
        SignalTestHelper.setUpdatedAt(lastSignal, LocalDateTime.now().minusDays(6))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
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
        val toMemberId = 2L
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal(fromMember = fromMember, toMember = toMember, status = SignalStatus.REJECTED)
        SignalTestHelper.setCreatedAt(lastSignal, LocalDateTime.now().minusDays(10))
        SignalTestHelper.setUpdatedAt(lastSignal, LocalDateTime.now().minusDays(8))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalJpaRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)
        val savedSignal = Signal(fromMember = fromMember, toMember = toMember)
        given(signalJpaRepository.save(any())).willReturn(savedSignal)

        // when & then
        val sendSignal = signalService.sendSignal(fromMember, toMemberId)
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
        val acceptedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.ACCEPTED)
        val rejectedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findByToMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(pendingSignal))

        // when
        val result = signalService.getReceivedSignals(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(pendingSignal)
        assertThat(result.content).doesNotContain(acceptedSignal, rejectedSignal)
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
        val acceptedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.ACCEPTED)
        val rejectedSignal = Signal(fromMember = fromMember2, toMember = me, status = SignalStatus.REJECTED)
        given(signalJpaRepository.findByFromMemberAndStatus(me, SignalStatus.PENDING)).willReturn(listOf(pendingSignal))

        // when
        val result = signalService.getSendSignalByMe(me, 0, 10)

        // then
        assertThat(result.content).containsExactly(pendingSignal)
        assertThat(result.content).doesNotContain(acceptedSignal, rejectedSignal)
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
} 