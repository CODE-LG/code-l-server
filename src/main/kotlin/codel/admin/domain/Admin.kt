package codel.admin.domain

import codel.admin.exception.AdminException
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import org.springframework.http.HttpStatus

class Admin(
    val password: String,
    val oauthType: OauthType = OauthType.ADMIN,
    val oauthId: String = "admin",
    val memberStatus: MemberStatus = MemberStatus.DONE,
) {
    fun validatePassword(targetPassword: String) {
        if (password != targetPassword) {
            throw AdminException(HttpStatus.UNAUTHORIZED, "패스워드가 일치하지 않습니다.")
        }
    }
}
