package codel.recommendation.domain

/**
 * 추천 타입
 */
enum class RecommendationType {
    /**
     * 오늘의 코드매칭 (24시간 유지)
     */
    DAILY_CODE_MATCHING,
    
    /**
     * 코드타임 (10시, 22시 시간대별)
     */
    CODE_TIME
}
