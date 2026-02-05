package codel.recommendation.business

import codel.member.domain.Member
import codel.recommendation.domain.AgePreference
import codel.recommendation.domain.RecommendationConfig
import org.springframework.stereotype.Component

/**
 * 나이 선호도 설정을 조회하는 Resolver
 *
 * 현재는 서비스 전역 설정(Config)에서 가져오지만,
 * 미래에는 회원별 개인 설정으로 확장 가능
 *
 * @see AgePreference
 */
@Component
class AgePreferenceResolver(
    private val recommendationConfig: RecommendationConfig
) {

    /**
     * 특정 회원의 나이 선호도 설정을 조회
     *
     * Phase 1: Config 기본값 반환
     * Phase 2 (미래): 회원 설정이 있으면 우선 사용, 없으면 Config fallback
     *
     * @param member 대상 회원
     * @return 해당 회원에게 적용할 나이 선호도 설정
     */
    fun resolve(member: Member): AgePreference {
        // Phase 1: Config 기본값 반환
        // TODO: Phase 2에서 회원별 설정 테이블 조회 로직 추가
        // val memberPreference = memberPreferenceRepository.findByMemberId(member.id)
        // if (memberPreference != null) {
        //     return AgePreference(
        //         preferredMaxDiff = memberPreference.agePreferredMaxDiff,
        //         cutoffDiff = memberPreference.ageCutoffDiff,
        //         allowCutoffWhenInsufficient = memberPreference.ageAllowCutoffWhenInsufficient
        //     )
        // }

        return AgePreference(
            preferredMaxDiff = recommendationConfig.agePreferredMaxDiff,
            cutoffDiff = recommendationConfig.ageCutoffDiff,
            allowCutoffWhenInsufficient = recommendationConfig.ageAllowCutoffWhenInsufficient
        )
    }

    /**
     * 기본 나이 선호도 설정 조회 (회원 정보 없이)
     *
     * @return Config 기본 나이 선호도 설정
     */
    fun resolveDefault(): AgePreference {
        return AgePreference(
            preferredMaxDiff = recommendationConfig.agePreferredMaxDiff,
            cutoffDiff = recommendationConfig.ageCutoffDiff,
            allowCutoffWhenInsufficient = recommendationConfig.ageAllowCutoffWhenInsufficient
        )
    }
}
