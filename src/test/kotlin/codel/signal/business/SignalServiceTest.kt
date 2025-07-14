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
} 