package codel.recommendation.domain

import codel.member.domain.Member

/**
 * 코드타임 추천 결과 DTO
 * 
 * 시간대별 추천 결과와 상태 정보를 담는 데이터 클래스
 */
data class CodeTimeRecommendationResult(
    /**
     * 현재 시간대 (예: "10:00", "22:00")
     * null이면 코드타임 시간대가 아님
     */
    val timeSlot: String?,
    
    /**
     * 추천된 사용자 목록
     * 빈 리스트면 추천 대상이 없거나 비활성 시간대
     */
    val members: List<Member>,
    
    /**
     * 현재 코드타임 활성 시간대 여부
     * true: 현재 시간이 코드타임 시간대
     * false: 비활성 시간대
     */
    val isActiveTime: Boolean,
    
    /**
     * 다음 코드타임 시간대 정보
     * 사용자에게 다음 시간대를 알려주기 위한 용도
     */
    val nextTimeSlot: String?
) {
    
    /**
     * 추천 결과 개수
     */
    val recommendationCount: Int
        get() = members.size
    
    /**
     * 추천 결과가 있는지 확인
     */
    val hasRecommendations: Boolean
        get() = members.isNotEmpty()
    
    /**
     * 현재 활성 시간대이면서 추천 결과가 있는지 확인
     */
    val isValidRecommendation: Boolean
        get() = isActiveTime && hasRecommendations
    
    companion object {
        /**
         * 비활성 시간대용 빈 결과 생성
         */
        fun createInactiveResult(nextTimeSlot: String?): CodeTimeRecommendationResult {
            return CodeTimeRecommendationResult(
                timeSlot = null,
                members = emptyList(),
                isActiveTime = false,
                nextTimeSlot = nextTimeSlot
            )
        }
        
        /**
         * 활성 시간대이지만 추천 결과가 없는 경우
         */
        fun createEmptyActiveResult(timeSlot: String, nextTimeSlot: String?): CodeTimeRecommendationResult {
            return CodeTimeRecommendationResult(
                timeSlot = timeSlot,
                members = emptyList(),
                isActiveTime = true,
                nextTimeSlot = nextTimeSlot
            )
        }
    }
}
