package codel.member.business.signup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class SignupStrategyResolverTest {

    private lateinit var postVerificationStrategy: PostVerificationStrategy
    private lateinit var preVerificationStrategy: PreVerificationStrategy
    private lateinit var resolver: SignupStrategyResolver

    @BeforeEach
    fun setUp() {
        postVerificationStrategy = mock(PostVerificationStrategy::class.java)
        preVerificationStrategy = mock(PreVerificationStrategy::class.java)
        resolver = SignupStrategyResolver(postVerificationStrategy, preVerificationStrategy)
    }

    @DisplayName("앱 버전이 1.2.0 이상이면 PostVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_2_0() {
        // given
        val appVersion = "1.2.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(postVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 1.5.0이면 PostVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_5_0() {
        // given
        val appVersion = "1.5.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(postVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 2.0.0이면 PostVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_2_0_0() {
        // given
        val appVersion = "2.0.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(postVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 1.1.9이면 PreVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_1_9() {
        // given
        val appVersion = "1.1.9"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 1.0.0이면 PreVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_0_0() {
        // given
        val appVersion = "1.0.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 null이면 PreVerificationStrategy를 반환한다 (하위호환)")
    @Test
    fun resolveStrategy_version_null() {
        // given
        val appVersion: String? = null

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }

    @DisplayName("앱 버전 파싱 실패 시 PreVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_invalid_version() {
        // given
        val appVersion = "invalid-version"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 빈 문자열이면 PreVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_empty_version() {
        // given
        val appVersion = ""

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 1.2 형식이면 정상 파싱하여 PostVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_2() {
        // given
        val appVersion = "1.2"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(postVerificationStrategy, result)
    }

    @DisplayName("앱 버전이 1.1 형식이면 정상 파싱하여 PreVerificationStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_1() {
        // given
        val appVersion = "1.1"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(preVerificationStrategy, result)
    }
}
