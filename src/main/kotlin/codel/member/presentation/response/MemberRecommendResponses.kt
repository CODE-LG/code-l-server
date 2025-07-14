package codel.member.presentation.response

import codel.member.domain.Member

data class MemberRecommendResponses(
    val content: List<MemberResponse>,
) {
    companion object {
        fun toResponse(members: List<Member>): MemberRecommendResponses {
            val responses =
                members
                    .stream()
                    .map { member -> MemberResponse.toResponse(member) }
                    .toList()
            return MemberRecommendResponses(responses)
        }
    }
}
