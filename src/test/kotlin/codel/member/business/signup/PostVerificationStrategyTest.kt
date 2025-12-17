package codel.member.business.signup

import codel.member.business.SignupService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockMultipartFile

class PostVerificationStrategyTest {

    private lateinit var signupService: SignupService
    private lateinit var strategy: PostVerificationStrategy

    @BeforeEach
    fun setUp() {
        signupService = mock(SignupService::class.java)
        strategy = PostVerificationStrategy(signupService)
    }

    @DisplayName("히든 이미지를 등록한다")
    @Test
    fun handleHiddenImages_registerImages() {
        // given
        val member = Member(
            id = 1L,
            oauthId = "test-oauth-id",
            oauthType = OauthType.KAKAO,
            memberStatus = MemberStatus.PERSONALITY_COMPLETED,
            email = "test@test.com"
        )

        val images = listOf(
            MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test1".toByteArray()),
            MockMultipartFile("image2", "test2.jpg", "image/jpeg", "test2".toByteArray()),
            MockMultipartFile("image3", "test3.jpg", "image/jpeg", "test3".toByteArray())
        )

        // when
        val response = strategy.handleHiddenImages(member, images)

        // then
        verify(signupService, times(1)).registerHiddenImages(member, images)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @DisplayName("회원 상태를 변경하지 않는다")
    @Test
    fun handleHiddenImages_noStatusChange() {
        // given
        val initialStatus = MemberStatus.PERSONALITY_COMPLETED
        val member = Member(
            id = 1L,
            oauthId = "test-oauth-id",
            oauthType = OauthType.KAKAO,
            memberStatus = initialStatus,
            email = "test@test.com"
        )

        val images = listOf(
            MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test1".toByteArray())
        )

        // when
        strategy.handleHiddenImages(member, images)

        // then
        assertEquals(initialStatus, member.memberStatus)
    }

    @DisplayName("다양한 회원 상태에서 모두 동일하게 동작한다")
    @Test
    fun handleHiddenImages_differentMemberStatuses() {
        // given
        val statuses = listOf(
            MemberStatus.PERSONALITY_COMPLETED,
            MemberStatus.REJECT,
            MemberStatus.PENDING,
            MemberStatus.DONE
        )

        val images = listOf(
            MockMultipartFile("image1", "test1.jpg", "image/jpeg", "test1".toByteArray())
        )

        // when & then
        statuses.forEach { status ->
            val member = Member(
                id = 1L,
                oauthId = "test-oauth-id",
                oauthType = OauthType.KAKAO,
                memberStatus = status,
                email = "test@test.com"
            )

            val response = strategy.handleHiddenImages(member, images)

            verify(signupService, times(1)).registerHiddenImages(member, images)
            assertEquals(status, member.memberStatus) // 상태 유지
            assertEquals(HttpStatus.OK, response.statusCode)

            reset(signupService)
        }
    }
}
