package codel.chat.business.strategy

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock

class QuestionRecommendStrategyResolverTest {

    private lateinit var categoryBasedStrategy: CategoryBasedQuestionStrategy
    private lateinit var legacyRandomStrategy: LegacyRandomQuestionStrategy
    private lateinit var resolver: QuestionRecommendStrategyResolver

    @BeforeEach
    fun setUp() {
        categoryBasedStrategy = mock(CategoryBasedQuestionStrategy::class.java)
        legacyRandomStrategy = mock(LegacyRandomQuestionStrategy::class.java)
        resolver = QuestionRecommendStrategyResolver(categoryBasedStrategy, legacyRandomStrategy)
    }

    @DisplayName("앱 버전이 1.3.0 이상이면 CategoryBasedQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_3_0() {
        // given
        val appVersion = "1.3.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(categoryBasedStrategy, result)
    }

    @DisplayName("앱 버전이 1.5.0이면 CategoryBasedQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_5_0() {
        // given
        val appVersion = "1.5.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(categoryBasedStrategy, result)
    }

    @DisplayName("앱 버전이 2.0.0이면 CategoryBasedQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_2_0_0() {
        // given
        val appVersion = "2.0.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(categoryBasedStrategy, result)
    }

    @DisplayName("앱 버전이 1.2.9이면 LegacyRandomQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_2_9() {
        // given
        val appVersion = "1.2.9"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전이 1.0.0이면 LegacyRandomQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_0_0() {
        // given
        val appVersion = "1.0.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전이 null이면 LegacyRandomQuestionStrategy를 반환한다 (하위호환)")
    @Test
    fun resolveStrategy_version_null() {
        // given
        val appVersion: String? = null

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전 파싱 실패 시 LegacyRandomQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_invalid_version() {
        // given
        val appVersion = "invalid-version"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전이 빈 문자열이면 LegacyRandomQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_empty_version() {
        // given
        val appVersion = ""

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전이 1.3 형식이면 정상 파싱하여 CategoryBasedQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_3() {
        // given
        val appVersion = "1.3"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(categoryBasedStrategy, result)
    }

    @DisplayName("앱 버전이 1.2 형식이면 정상 파싱하여 LegacyRandomQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_1_2() {
        // given
        val appVersion = "1.2"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(legacyRandomStrategy, result)
    }

    @DisplayName("앱 버전이 10.0.0이면 CategoryBasedQuestionStrategy를 반환한다")
    @Test
    fun resolveStrategy_version_10_0_0() {
        // given
        val appVersion = "10.0.0"

        // when
        val result = resolver.resolveStrategy(appVersion)

        // then
        assertEquals(categoryBasedStrategy, result)
    }
}
