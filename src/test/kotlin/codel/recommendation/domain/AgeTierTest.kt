package codel.recommendation.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("AgeTier 테스트")
class AgeTierTest {

    @Nested
    @DisplayName("from 메서드")
    inner class FromTest {

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2])
        @DisplayName("나이 차이 0~2살이면 A1 반환")
        fun ageDiff0to2ReturnsA1(ageDiff: Int) {
            // when
            val tier = AgeTier.from(ageDiff)

            // then
            assertEquals(AgeTier.A1, tier)
        }

        @ParameterizedTest
        @ValueSource(ints = [3, 4, 5])
        @DisplayName("나이 차이 3~5살이면 A2 반환")
        fun ageDiff3to5ReturnsA2(ageDiff: Int) {
            // when
            val tier = AgeTier.from(ageDiff)

            // then
            assertEquals(AgeTier.A2, tier)
        }

        @ParameterizedTest
        @ValueSource(ints = [6, 7, 10, 20, 100])
        @DisplayName("나이 차이 6살 이상이면 A3 반환")
        fun ageDiff6PlusReturnsA3(ageDiff: Int) {
            // when
            val tier = AgeTier.from(ageDiff)

            // then
            assertEquals(AgeTier.A3, tier)
        }

        @Test
        @DisplayName("음수 나이 차이는 예외 발생")
        fun negativeAgeDiffThrowsException() {
            // when & then
            assertThrows(IllegalArgumentException::class.java) {
                AgeTier.from(-1)
            }
        }
    }

    @Nested
    @DisplayName("isPreferred 메서드")
    inner class IsPreferredTest {

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        @DisplayName("0~5살 차이면 선호 범위(true)")
        fun preferredRangeReturnsTrue(ageDiff: Int) {
            assertTrue(AgeTier.isPreferred(ageDiff))
        }

        @ParameterizedTest
        @ValueSource(ints = [6, 7, 10, 20])
        @DisplayName("6살 이상 차이면 선호 범위 아님(false)")
        fun nonPreferredRangeReturnsFalse(ageDiff: Int) {
            assertFalse(AgeTier.isPreferred(ageDiff))
        }
    }

    @Nested
    @DisplayName("isCutoff 메서드")
    inner class IsCutoffTest {

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        @DisplayName("0~5살 차이면 컷오프 아님(false)")
        fun preferredRangeReturnsFalse(ageDiff: Int) {
            assertFalse(AgeTier.isCutoff(ageDiff))
        }

        @ParameterizedTest
        @ValueSource(ints = [6, 7, 10, 20])
        @DisplayName("6살 이상 차이면 컷오프 대상(true)")
        fun cutoffRangeReturnsTrue(ageDiff: Int) {
            assertTrue(AgeTier.isCutoff(ageDiff))
        }
    }

    @Nested
    @DisplayName("우선순위")
    inner class PriorityTest {

        @Test
        @DisplayName("A1의 우선순위가 가장 높다 (숫자가 작음)")
        fun a1HasHighestPriority() {
            assertTrue(AgeTier.A1.priority < AgeTier.A2.priority)
            assertTrue(AgeTier.A2.priority < AgeTier.A3.priority)
        }

        @ParameterizedTest
        @CsvSource(
            "A1, 1",
            "A2, 2",
            "A3, 3"
        )
        @DisplayName("각 Tier의 우선순위 값 확인")
        fun tierPriorityValues(tierName: String, expectedPriority: Int) {
            val tier = AgeTier.valueOf(tierName)
            assertEquals(expectedPriority, tier.priority)
        }
    }
}