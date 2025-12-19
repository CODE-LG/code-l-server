package codel.recommendation.presentation.response

import codel.recommendation.business.RecommendationOverview
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 추천 현황 종합 응답 DTO
 */
data class RecommendationOverviewResponse(
    @JsonProperty("userId")
    val userId: Long,
    
    @JsonProperty("hasDailyCodeMatching")
    val hasDailyCodeMatching: Boolean,
    
    @JsonProperty("currentTimeSlot")
    val currentTimeSlot: String?,
    
    @JsonProperty("nextTimeSlot") 
    val nextTimeSlot: String?,
    
    @JsonProperty("isCodeTimeActive")
    val isCodeTimeActive: Boolean,
    
    @JsonProperty("dailyCodeMatchingStats")
    val dailyCodeMatchingStats: Map<String, Any>,
    
    @JsonProperty("codeTimeStats")
    val codeTimeStats: Map<String, Any>,
    
    @JsonProperty("allCodeTimeResults")
    val allCodeTimeResults: Map<String, CodeTimeData>,
    
    @JsonProperty("bucketStatistics")
    val bucketStatistics: Map<String, Int>,
    
    @JsonProperty("totalUniqueRecommendationCount")
    val totalUniqueRecommendationCount: Long
) {
    companion object {
        fun from(overview: RecommendationOverview): RecommendationOverviewResponse {
            return RecommendationOverviewResponse(
                userId = overview.userId,
                hasDailyCodeMatching = overview.hasDailyCodeMatching,
                currentTimeSlot = overview.currentTimeSlot,
                nextTimeSlot = overview.nextTimeSlot,
                isCodeTimeActive = overview.isCodeTimeActive,
                dailyCodeMatchingStats = overview.dailyCodeMatchingStats,
                codeTimeStats = overview.codeTimeStats,
                allCodeTimeResults = overview.allCodeTimeResults.mapValues { (_, result) ->
                    CodeTimeData.from(result)
                },
                bucketStatistics = overview.bucketStatistics,
                totalUniqueRecommendationCount = overview.totalUniqueRecommendationCount
            )
        }
    }
}
