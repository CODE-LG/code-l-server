package codel.signal.business

import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import codel.signal.infrastructure.SignalRepository
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

@ExtendWith(MockitoExtension::class)
class SignalServiceTest {
    @Mock
    lateinit var memberRepository: MemberRepository
    @Mock
    lateinit var signalRepository: SignalRepository
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
        given(signalRepository.save(any())).willReturn(savedSignal)

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
        val lastSignal = Signal.testInstance(fromMember = fromMember, toMember = toMember, status = SignalStatus.PENDING, createdAt = LocalDateTime.now().minusDays(1))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

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
        val lastSignal = Signal.testInstance(fromMember = fromMember, toMember = toMember, status = SignalStatus.ACCEPTED, createdAt = LocalDateTime.now().minusDays(1))
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("이미 시그널을 보낸 상대입니다.")
    }

    @DisplayName("REJECTED 상태지만 15일이 지나지 않았으면 예외가 발생한다")
    @Test
    fun sendSignal_rejected_within_15days_fail() {
        // given
        val fromMember = mock(Member::class.java)
        val toMember = mock(Member::class.java)
        val toMemberId = 2L
        given(fromMember.id).willReturn(1L)
        val lastSignal = Signal.testInstance(
            fromMember = fromMember,
            toMember = toMember,
            status = SignalStatus.REJECTED,
            createdAt = LocalDateTime.now().minusDays(10) // 테스트 전용 생성자 사용
        )
        given(memberRepository.findMember(toMemberId)).willReturn(toMember)
        given(signalRepository.findTopByFromMemberAndToMemberOrderByIdDesc(fromMember, toMember)).willReturn(lastSignal)

        // when & then
        val exception = assertThrows<SignalException> {
            signalService.sendSignal(fromMember, toMemberId)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("거절된 상대에게는 15일 후에 다시 시그널을 보낼 수 있습니다.")
    }
} 