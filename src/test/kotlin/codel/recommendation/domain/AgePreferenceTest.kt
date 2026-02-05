package codel.recommendation.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("AgePreference 테스트")
class AgePreferenceTest {

    @Nested
    @DisplayName("생성자 검증")
    inner class ConstructorValidationTest {

        @Test
        @DisplayName("기본값으로 생성 가능")
        fun createWithDefaults() {
            // when
            val pref = AgePreference()

            // then
            assertEquals(5, pref.preferredMaxDiff)
            assertEquals(6, pref.cutoffDiff)
            assertTrue(pref.allowCutoffWhenInsufficient)
        }

        @Test
        @DisplayName("커스텀 값으로 생성 가능")
        fun createWithCustomValues() {
            // when
            val pref = AgePreference(
                preferredMaxDiff = 3,
                cutoffDiff = 10,
                allowCutoffWhenInsufficient = false
            )

            // then
            assertEquals(3, pref.preferredMaxDiff)
            assertEquals(10, pref.cutoffDiff)
            assertFalse(pref.allowCutoffWhenInsufficient)
        }

        @Test
        @DisplayName("preferredMaxDiff가 음수면 예외 발생")
        fun negativePreferredMaxDiffThrowsException() {
            assertThrows(IllegalArgumentException::class.java) {
                AgePreference(preferredMaxDiff = -1, cutoffDiff = 6)
            }
        }

        @Test
        @DisplayName("cutoffDiff가 preferredMaxDiff 이하면 예외 발생")
        fun cutoffLessThanPreferredThrowsException() {
            assertThrows(IllegalArgumentException::class.java) {
                AgePreference(preferredMaxDiff = 5, cutoffDiff = 5)
            }

            assertThrows(IllegalArgumentException::class.java) {
                AgePreference(preferredMaxDiff = 5, cutoffDiff = 3)
            }
        }
    }

    @Nested
    @DisplayName("isPreferred 메서드")
    inner class IsPreferredTest {

        private val pref = AgePreference(preferredMaxDiff = 5, cutoffDiff = 6)

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        @DisplayName("preferredMaxDiff 이하면 true")
        fun withinPreferredRangeReturnsTrue(ageDiff: Int) {
            assertTrue(pref.isPreferred(ageDiff))
        }

        @ParameterizedTest
        @ValueSource(ints = [6, 7, 10, 20])
        @DisplayName("preferredMaxDiff 초과면 false")
        fun exceedsPreferredRangeReturnsFalse(ageDiff: Int) {
            assertFalse(pref.isPreferred(ageDiff))
        }
    }

    @Nested
    @DisplayName("isCutoff 메서드")
    inner class IsCutoffTest {

        private val pref = AgePreference(preferredMaxDiff = 5, cutoffDiff = 6)

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        @DisplayName("cutoffDiff 미만이면 false")
        fun belowCutoffReturnsFalse(ageDiff: Int) {
            assertFalse(pref.isCutoff(ageDiff))
        }

        @ParameterizedTest
        @ValueSource(ints = [6, 7, 10, 20])
        @DisplayName("cutoffDiff 이상이면 true")
        fun atOrAboveCutoffReturnsTrue(ageDiff: Int) {
            assertTrue(pref.isCutoff(ageDiff))
        }
    }

    @Nested
    @DisplayName("isPreferredAge 메서드")
    inner class IsPreferredAgeTest {

        private val pref = AgePreference(preferredMaxDiff = 5, cutoffDiff = 6)

        @ParameterizedTest
        @CsvSource(
            "26, 26, true",  // 0살 차이
            "26, 28, true",  // 2살 차이
            "26, 31, true",  // 5살 차이
            "26, 21, true",  // 5살 차이 (반대)
            "26, 32, false", // 6살 차이
            "26, 33, false", // 7살 차이
            "26, 20, false"  // 6살 차이 (반대)
        )
        @DisplayName("두 나이 간 선호 범위 확인")
        fun checkPreferredAge(userAge: Int, targetAge: Int, expected: Boolean) {
            assertEquals(expected, pref.isPreferredAge(userAge, targetAge))
        }
    }

    @Nested
    @DisplayName("isCutoffAge 메서드")
    inner class IsCutoffAgeTest {

        private val pref = AgePreference(preferredMaxDiff = 5, cutoffDiff = 6)

        @ParameterizedTest
        @CsvSource(
            "26, 26, false", // 0살 차이
            "26, 31, false", // 5살 차이
            "26, 32, true",  // 6살 차이
            "26, 33, true",  // 7살 차이
            "26, 20, true"   // 6살 차이 (반대)
        )
        @DisplayName("두 나이 간 컷오프 대상 확인")
        fun checkCutoffAge(userAge: Int, targetAge: Int, expected: Boolean) {
            assertEquals(expected, pref.isCutoffAge(userAge, targetAge))
        }
    }

    @Nested
    @DisplayName("default 팩토리 메서드")
    inner class DefaultFactoryTest {

        @Test
        @DisplayName("기본값과 동일한 설정 반환")
        fun defaultReturnsDefaultValues() {
            // when
            val pref = AgePreference.default()

            // then
            assertEquals(5, pref.preferredMaxDiff)
            assertEquals(6, pref.cutoffDiff)
            assertTrue(pref.allowCutoffWhenInsufficient)
        }
    }
}