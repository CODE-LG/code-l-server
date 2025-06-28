package codel.member.presentation.response

import codel.member.domain.Member
import kotlin.streams.toList

data class MemberRecommendResponses(
    val responses: List<MemberRecommendResponse>,
) {
    companion object {
        fun toResponse(members: List<Member>): MemberRecommendResponses {
            val responses =
                members
                    .stream()
                    .map { member -> MemberRecommendResponse.toResponse(member) }
                    .toList()
            return MemberRecommendResponses(responses)
        }
    }
}
