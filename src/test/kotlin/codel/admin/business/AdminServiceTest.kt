package codel.admin.business

import codel.admin.domain.Admin
import codel.admin.domain.ValidateCode
import codel.admin.exception.AdminException
import codel.config.TestFixture
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class AdminServiceTest(
    @Autowired val adminService: AdminService,
    @Autowired val memberService: MemberService,
    @Value("\${security.admin.password}")
    private val answerPassword: String,
) : TestFixture() {
    @DisplayName("정확한 비밀번호를 작성하면 로그인에 성공한다.")
    @Test
    fun loginAdminSuccessTest() {
        val admin = Admin(password = answerPassword)
        Assertions.assertDoesNotThrow { adminService.loginAdmin(admin) }
    }

    @DisplayName("정확하지 않은 비밀번호를 작성하면 로그인에 실패한다.")
    @Test
    fun loginAdminFailureTest() {
        val admin = Admin(password = "wrong password")
        Assertions.assertThrows(AdminException::class.java) { adminService.loginAdmin(admin) }
    }

    @DisplayName("PENDING 상태인 회원을 찾아온다.")
    @Test
    fun findPendingMembersTest() {
        val pendingMembers = adminService.findPendingMembers()

        Assertions.assertAll(
            { Assertions.assertEquals(1, pendingMembers.size) },
            { Assertions.assertEquals(memberPending.id, pendingMembers[0].id) },
        )
    }

    @DisplayName("사용자의 프로필을 승인하면 사용자의 상태가 DONE 으로 바뀐다.")
    @Test
    fun reviewMemberProfileApproveTest() {
        val target =
            Member(
                oauthType = memberPending.oauthType,
                oauthId = memberPending.oauthId,
            )

        adminService.reviewMemberProfile(target, ValidateCode.APPROVE)

        val findMember =
            memberService.findMember(
                oauthType = memberPending.oauthType,
                oauthId = memberPending.oauthId,
            )
        assertThat(findMember.memberStatus).isEqualTo(MemberStatus.DONE)
    }

    @DisplayName("사용자의 프로필을 거절하면 사용자의 상태가 REJECT 로 바뀐다.")
    @Test
    fun reviewMemberProfileRejectTest() {
        val target =
            Member(
                oauthType = memberPending.oauthType,
                oauthId = memberPending.oauthId,
            )

        adminService.reviewMemberProfile(target, ValidateCode.REJECT)

        val findMember =
            memberService.findMember(
                oauthType = memberPending.oauthType,
                oauthId = memberPending.oauthId,
            )
        assertThat(findMember.memberStatus).isEqualTo(MemberStatus.REJECT)
    }
}
