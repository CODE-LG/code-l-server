package codel.member.presentation.response

import codel.member.domain.Member

/**
 * 추천 멤버 목록 응답
 * 추천/파도타기에서는 히든 프로필 접근 불가능한 대상만 표시되므로 FullProfileResponse.createOpen 사용
 */
data class MemberRecommendResponse(
    val members: List<FullProfileResponse>
) {
    companion object {
        fun from(members: List<Member>): MemberRecommendResponse {
            return MemberRecommendResponse(
                members = members.map { FullProfileResponse.createOpen(it) }
            )
        }
    }
}
