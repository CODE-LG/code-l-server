package codel.recommendation.business

import codel.member.domain.Member
import codel.recommendation.domain.AgePreference
import codel.recommendation.domain.RecommendationConfig
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

@DisplayName("AgePreferenceResolver 테스트")
class AgePreferenceResolverTest {

    private lateinit var recommendationConfig: RecommendationConfig
    private lateinit var resolver: AgePreferenceResolver

    @BeforeEach
    fun setUp() {
        recommendationConfig = mock(RecommendationConfig::class.java)
        resolver = AgePreferenceResolver(recommendationConfig)
    }

    @Nested
    @DisplayName("resolve 메서드")
    inner class ResolveTest {

        @Test
        @DisplayName("Config 기본값을 반환한다")
        fun returnsConfigValues() {
            // given
            val member = mock(Member::class.java)
            `when`(recommendationConfig.agePreferredMaxDiff).thenReturn(5)
            `when`(recommendationConfig.ageCutoffDiff).thenReturn(6)
            `when`(recommendationConfig.ageAllowCutoffWhenInsufficient).thenReturn(true)

            // when
            val result = resolver.resolve(member)

            // then
            assertEquals(5, result.preferredMaxDiff)
            assertEquals(6, result.cutoffDiff)
            assertTrue(result.allowCutoffWhenInsufficient)
        }

        @Test
        @DisplayName("커스텀 Config 값을 반환한다")
        fun returnsCustomConfigValues() {
            // given
            val member = mock(Member::class.java)
            `when`(recommendationConfig.agePreferredMaxDiff).thenReturn(3)
            `when`(recommendationConfig.ageCutoffDiff).thenReturn(10)
            `when`(recommendationConfig.ageAllowCutoffWhenInsufficient).thenReturn(false)

            // when
            val result = resolver.resolve(member)

            // then
            assertEquals(3, result.preferredMaxDiff)
            assertEquals(10, result.cutoffDiff)
            assertFalse(result.allowCutoffWhenInsufficient)
        }
    }

    @Nested
    @DisplayName("resolveDefault 메서드")
    inner class ResolveDefaultTest {

        @Test
        @DisplayName("회원 정보 없이 Config 값을 반환한다")
        fun returnsConfigValuesWithoutMember() {
            // given
            `when`(recommendationConfig.agePreferredMaxDiff).thenReturn(5)
            `when`(recommendationConfig.ageCutoffDiff).thenReturn(6)
            `when`(recommendationConfig.ageAllowCutoffWhenInsufficient).thenReturn(true)

            // when
            val result = resolver.resolveDefault()

            // then
            assertEquals(5, result.preferredMaxDiff)
            assertEquals(6, result.cutoffDiff)
            assertTrue(result.allowCutoffWhenInsufficient)
        }
    }
}
