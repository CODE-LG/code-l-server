package codel.recommendation.presentation.response

import codel.recommendation.domain.CodeTimeRecommendationResult
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 코드타임 응답 DTO
 */
data class CodeTimeResponse(
    @JsonProperty("timeSlot")
    val timeSlot: String?,
    
    @JsonProperty("members")
    val members: List<MemberSummary>,
    
    @JsonProperty("count")
    val count: Int,
    
    @JsonProperty("isActiveTime")
    val isActiveTime: Boolean,
    
    @JsonProperty("nextTimeSlot")
    val nextTimeSlot: String?,
    
    @JsonProperty("message")
    val message: String
) {
    companion object {
        fun from(result: CodeTimeRecommendationResult): CodeTimeResponse {
            val message = when {
                !result.isActiveTime -> {
                    if (result.nextTimeSlot != null) {
                        "현재 코드타임 시간대가 아닙니다. 다음 코드타임은 ${result.nextTimeSlot}입니다."
                    } else {
                        "현재 코드타임 시간대가 아닙니다."
                    }
                }
                result.hasRecommendations -> {
                    "코드타임 ${result.timeSlot}에 ${result.recommendationCount}명을 추천드립니다!"
                }
                else -> {
                    "현재 추천 가능한 사용자가 없습니다."
                }
            }
            
            return CodeTimeResponse(
                timeSlot = result.timeSlot,
                members = result.members.map { MemberSummary.from(it) },
                count = result.recommendationCount,
                isActiveTime = result.isActiveTime,
                nextTimeSlot = result.nextTimeSlot,
                message = message
            )
        }
    }
}
