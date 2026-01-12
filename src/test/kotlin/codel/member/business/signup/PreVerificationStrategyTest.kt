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
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

class PreVerificationStrategyTest {

    private lateinit var signupService: SignupService
    private lateinit var memberJpaRepository: MemberJpaRepository
    private lateinit var strategy: PreVerificationStrategy
    private lateinit var asyncNotificationService: IAsyncNotificationService

    @BeforeEach
    fun setUp() {
        signupService = mock(SignupService::class.java)
        memberJpaRepository = mock(MemberJpaRepository::class.java)
        asyncNotificationService = mock(IAsyncNotificationService::class.java)
        strategy = PreVerificationStrategy(signupService, memberJpaRepository, asyncNotificationService)
    }

    @DisplayName("PERSONALITY_COMPLETED 상태에서는 히든 이미지 등록 후 HIDDEN_COMPLETED 상태로 변경한다")
    @Test
    fun handleHiddenImages_personalityCompleted_changeToHiddenCompleted() {
        // given
        val profile = Profile(
            codeName = "테스트유저"
        )

        val member = Member(
            id = 1L,
            oauthId = "test-oauth-id",
            oauthType = OauthType.KAKAO,
            memberStatus = MemberStatus.PERSONALITY_COMPLETED,
            email = "test@test.com"
        )

        // 양방향 연관관계 설정
        member.updateProfile(profile)

        val images = listOf(
            MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test1".toByteArray()),
            MockMultipartFile("image2", "test2.jpg", "image/jpeg", "test2".toByteArray()),
            MockMultipartFile("image3", "test3.jpg", "image/jpeg", "test3".toByteArray())
        )

        `when`(memberJpaRepository.findByMemberId(1L)).thenReturn(member)

        // when
        val response = strategy.handleHiddenImages(member, images)

        // then
        verify(signupService, times(1)).registerHiddenImages(member, images)
        verify(memberJpaRepository, times(1)).findByMemberId(1L)

        // member 상태가 PENDING으로 변경되었는지 확인
        assertEquals(MemberStatus.PENDING, member.memberStatus)
        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
