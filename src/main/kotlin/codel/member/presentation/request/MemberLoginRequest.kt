package codel.member.presentation.request

import codel.member.domain.Member
import codel.member.domain.OauthType

data class MemberLoginRequest(
    val oauthType: OauthType,
    val oauthId: String,
    val email: String,
) {
    fun toMember(): Member =
        Member(
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            email = this.email,
        )
}
