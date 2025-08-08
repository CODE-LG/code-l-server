package codel.member.presentation.response

import codel.member.domain.MemberStatus

data class MemberLoginResponse(
    val memberId : Long,
    val memberStatus: MemberStatus,
)
