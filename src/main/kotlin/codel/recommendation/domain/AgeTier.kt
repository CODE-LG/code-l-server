package codel.recommendation.domain

/**
 * 나이 차이에 따른 추천 우선순위 Tier
 *
 * - A1: 0~2살 차이 (최우선 추천)
 * - A2: 3~5살 차이 (우선 추천)
 * - A3: 6살 이상 차이 (컷오프 대상, 부족 시에만 허용)
 */
enum class AgeTier(
    val minDiff: Int,
    val maxDiff: Int,
    val priority: Int
) {
    A1(0, 2, 1),
    A2(3, 5, 2),
    A3(6, Int.MAX_VALUE, 3);

    companion object {
        /**
         * 나이 차이로부터 해당하는 AgeTier를 반환
         */
        fun from(ageDiff: Int): AgeTier {
            require(ageDiff >= 0) { "나이 차이는 0 이상이어야 합니다: $ageDiff" }

            return when {
                ageDiff <= A1.maxDiff -> A1
                ageDiff <= A2.maxDiff -> A2
                else -> A3
            }
        }

        /**
         * 선호 범위 내인지 확인 (A1 또는 A2)
         */
        fun isPreferred(ageDiff: Int): Boolean {
            return from(ageDiff) != A3
        }

        /**
         * 컷오프 대상인지 확인 (A3)
         */
        fun isCutoff(ageDiff: Int): Boolean {
            return from(ageDiff) == A3
        }
    }
}
