package codel.member.business.signup

import codel.member.business.SignupService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.member.infrastructure.MemberJpaRepository
import codel.notification.business.IAsyncNotificationService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate

class PreVerificationStrategyTest {

    private lateinit var signupService: SignupService
    private lateinit var memberJpaRepository: MemberJpaRepository
    private lateinit var asyncNotificationService: IAsyncNotificationService
    private lateinit var strategy: PreVerificationStrategy

    @BeforeEach
    fun setUp() {
        signupService = mock(SignupService::class.java)
        memberJpaRepository = mock(MemberJpaRepository::class.java)
        asyncNotificationService = mock(IAsyncNotificationService::class.java)
        strategy = PreVerificationStrategy(signupService, memberJpaRepository, asyncNotificationService)
    }

    @DisplayName("PERSONALITY_COMPLETED 상태에서는 히든 이미지 등록 후 PENDING 상태로 변경한다")
    @Test
    fun handleHiddenImages_personalityCompleted_changeToPending() {
        // given
        val profile = Profile(
            id = 1L,
            codeName = "테스트유저",
            bigCity = "서울",
            smallCity = "강남구",
            birthDate = LocalDate.of(1990, 1, 1)
        )

        val member = Member(
            id = 1L,
            oauthId = "test-oauth-id",
            oauthType = OauthType.KAKAO,
            memberStatus = MemberStatus.PERSONALITY_COMPLETED,
            email = "test@test.com",
            profile = profile
        )

        profile.member = member

        val images = listOf(
            MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test1".toByteArray()),
            MockMultipartFile("image2", "test2.jpg", "image/jpeg", "test2".toByteArray()),
            MockMultipartFile("image3", "test3.jpg", "image/jpeg", "test3".toByteArray())
        )

        // memberJpaRepository.findByMemberId가 member를 반환하도록 mock 설정
        `when`(memberJpaRepository.findByMemberId(1L)).thenReturn(member)

        // when
        val response = strategy.handleHiddenImages(member, images)

        // then
        verify(signupService, times(1)).registerHiddenImages(member, images)
        verify(memberJpaRepository, times(1)).findByMemberId(1L)

        assertEquals(MemberStatus.PENDING, member.memberStatus)
        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
