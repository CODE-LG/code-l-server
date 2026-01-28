package codel.recommendation.domain

import kotlin.math.abs

/**
 * 나이 선호도 설정 Value Object
 *
 * 현재는 서비스 전역 설정(Config)에서 가져오지만,
 * 미래에는 회원별 개인 설정으로 확장 가능
 *
 * @property preferredMaxDiff 우선 추천 최대 나이 차이 (기본: 5)
 * @property cutoffDiff 컷오프 기준 나이 차이 (기본: 6, 이 값 이상이면 제외)
 * @property allowCutoffWhenInsufficient 후보 부족 시 컷오프 대상 허용 여부 (기본: true)
 */
data class AgePreference(
    val preferredMaxDiff: Int = 5,
    val cutoffDiff: Int = 6,
    val allowCutoffWhenInsufficient: Boolean = true
) {
    init {
        require(preferredMaxDiff >= 0) { "preferredMaxDiff는 0 이상이어야 합니다: $preferredMaxDiff" }
        require(cutoffDiff > preferredMaxDiff) { "cutoffDiff는 preferredMaxDiff보다 커야 합니다: cutoff=$cutoffDiff, preferred=$preferredMaxDiff" }
    }

    /**
     * 해당 나이 차이가 선호 범위 내인지 확인
     */
    fun isPreferred(ageDiff: Int): Boolean {
        return ageDiff <= preferredMaxDiff
    }

    /**
     * 해당 나이 차이가 컷오프 대상인지 확인
     */
    fun isCutoff(ageDiff: Int): Boolean {
        return ageDiff >= cutoffDiff
    }

    /**
     * 두 나이 사이의 차이가 선호 범위 내인지 확인
     */
    fun isPreferredAge(userAge: Int, targetAge: Int): Boolean {
        return isPreferred(abs(userAge - targetAge))
    }

    /**
     * 두 나이 사이의 차이가 컷오프 대상인지 확인
     */
    fun isCutoffAge(userAge: Int, targetAge: Int): Boolean {
        return isCutoff(abs(userAge - targetAge))
    }

    companion object {
        /**
         * 기본 설정 (기획서 R_V2 기준)
         */
        fun default(): AgePreference = AgePreference()
    }
}
