package codel.recommendation.presentation.response

import codel.member.domain.Member
import codel.recommendation.business.PrimaryRecommendation
import codel.recommendation.business.RecommendationResult
import codel.recommendation.domain.CodeTimeRecommendationResult
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 통합 추천 응답 DTO
 */
data class RecommendationResponse(
    @JsonProperty("primaryRecommendation")
    val primaryRecommendation: String,
    
    @JsonProperty("recommendationMessage")
    val recommendationMessage: String,
    
    @JsonProperty("dailyCodeMatching")
    val dailyCodeMatching: DailyCodeMatchingData?,
    
    @JsonProperty("codeTime")
    val codeTime: CodeTimeData?
) {
    companion object {
        fun from(result: RecommendationResult): RecommendationResponse {
            return RecommendationResponse(
                primaryRecommendation = when (result.primaryRecommendation) {
                    PrimaryRecommendation.DAILY_CODE_MATCHING -> "DAILY_CODE_MATCHING"
                    PrimaryRecommendation.CODE_TIME -> "CODE_TIME"
                },
                recommendationMessage = result.recommendationMessage,
                dailyCodeMatching = if (result.dailyCodeMatching.isNotEmpty()) {
                    DailyCodeMatchingData.from(result.dailyCodeMatching)
                } else null,
                codeTime = result.codeTimeResult?.let { CodeTimeData.from(it) }
            )
        }
    }
}

/**
 * 오늘의 코드매칭 데이터
 */
data class DailyCodeMatchingData(
    @JsonProperty("members")
    val members: List<MemberSummary>,
    
    @JsonProperty("count")
    val count: Int
) {
    companion object {
        fun from(members: List<Member>): DailyCodeMatchingData {
            return DailyCodeMatchingData(
                members = members.map { MemberSummary.from(it) },
                count = members.size
            )
        }
    }
}

/**
 * 코드타임 데이터
 */
data class CodeTimeData(
    @JsonProperty("timeSlot")
    val timeSlot: String?,
    
    @JsonProperty("members")
    val members: List<MemberSummary>,
    
    @JsonProperty("count")
    val count: Int,
    
    @JsonProperty("isActiveTime")
    val isActiveTime: Boolean,
    
    @JsonProperty("nextTimeSlot")
    val nextTimeSlot: String?
) {
    companion object {
        fun from(result: CodeTimeRecommendationResult): CodeTimeData {
            return CodeTimeData(
                timeSlot = result.timeSlot,
                members = result.members.map { MemberSummary.from(it) },
                count = result.recommendationCount,
                isActiveTime = result.isActiveTime,
                nextTimeSlot = result.nextTimeSlot
            )
        }
    }
}

/**
 * 회원 요약 정보
 */
data class MemberSummary(
    @JsonProperty("id")
    val id: Long,
    
    @JsonProperty("codeName")
    val codeName: String,
    
    @JsonProperty("age")
    val age: Int?,
    
    @JsonProperty("region")
    val region: String,
    
    @JsonProperty("profileImageUrl")
    val profileImageUrl: String?
) {
    companion object {
        fun from(member: Member): MemberSummary {
            val profile = member.getProfileOrThrow()
            return MemberSummary(
                id = member.getIdOrThrow(),
                codeName = profile.codeName ?: "익명",
                age = profile.getAge(),
                region = "${profile.bigCity ?: "미설정"}-${profile.smallCity ?: "미설정"}",
                profileImageUrl = profile.codeImage
            )
        }
    }
}
