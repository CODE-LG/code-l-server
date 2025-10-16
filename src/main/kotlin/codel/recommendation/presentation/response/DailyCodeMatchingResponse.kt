package codel.recommendation.presentation.response

import codel.member.domain.Member
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 오늘의 코드매칭 응답 DTO
 */
data class DailyCodeMatchingResponse(
    @JsonProperty("members")
    val members: List<MemberSummary>,
    
    @JsonProperty("count")
    val count: Int,
    
    @JsonProperty("message")
    val message: String
) {
    companion object {
        fun from(members: List<Member>): DailyCodeMatchingResponse {
            return DailyCodeMatchingResponse(
                members = members.map { MemberSummary.from(it) },
                count = members.size,
                message = if (members.isNotEmpty()) {
                    "오늘의 코드매칭 ${members.size}명을 추천드립니다!"
                } else {
                    "현재 추천 가능한 사용자가 없습니다."
                }
            )
        }
    }
}
