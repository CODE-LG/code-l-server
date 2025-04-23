package codel.admin.business

import codel.admin.domain.Admin
import codel.admin.domain.ValidateCode
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val memberService: MemberService,
    @Value("\${security.admin.password}")
    private val answerPassword: String,
) {
    fun loginAdmin(admin: Admin) {
        admin.validatePassword(answerPassword)

        val member =
            Member(
                oauthType = admin.oauthType,
                oauthId = admin.oauthId,
                memberStatus = admin.memberStatus,
            )

        memberService.loginMember(member)
    }

    fun findPendingMemberFaceImage(): List<Member> = memberService.findPendingMembers()

    @Transactional
    fun validateFaceImage(
        target: Member,
        request: ValidateCode,
    ) {
        val member =
            memberService.findMember(
                oauthType = target.oauthType,
                oauthId = target.oauthId,
            )

        val memberStatus =
            when (request) {
                ValidateCode.APPROVE -> MemberStatus.DONE
                ValidateCode.REJECT -> MemberStatus.REJECT
            }

        memberService.updateMemberStatus(member, memberStatus)
    }
}
