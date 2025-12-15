package codel.member.presentation.response

import codel.member.domain.MemberStatus

/**
 * 재심사 요청 응답
 */
data class ResubmitProfileResponse(
    val status: MemberStatus,
    val message: String
)
