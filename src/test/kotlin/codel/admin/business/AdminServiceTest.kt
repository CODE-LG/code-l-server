package codel.admin.business

import codel.admin.domain.Admin
import codel.admin.exception.AdminException
import codel.config.TestFixture
import codel.member.business.MemberService
import codel.member.domain.MemberStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
        assertDoesNotThrow { adminService.loginAdmin(admin) }
    }

    @DisplayName("정확하지 않은 비밀번호를 작성하면 로그인에 실패한다.")
    @Test
    fun loginAdminFailureTest() {
        val admin = Admin(password = "wrong password")
        assertThrows(AdminException::class.java) { adminService.loginAdmin(admin) }
    }

    @DisplayName("PENDING 상태인 회원을 찾아온다.")
    @Test
    fun findPendingMembersTest() {
        val pendingMembers = adminService.findPendingMembers()

        assertAll(
            { assertEquals(1, pendingMembers.size) },
            { assertEquals(memberPending.id, pendingMembers[0].id) },
        )
    }

    @DisplayName("사용자의 프로필을 승인하면 사용자의 상태가 DONE 으로 바뀐다.")
    @Test
    fun reviewMemberProfileApproveTest() {
        adminService.approveMemberProfile(memberPending.id!!)

        val findMember = memberService.findMember(memberId = memberPending.id!!)
        assertThat(findMember.memberStatus).isEqualTo(MemberStatus.DONE)
    }

    @DisplayName("사용자의 프로필을 거절하면 사용자의 상태가 REJECT 로 바뀐다.")
    @Test
    fun reviewMemberProfileRejectTest() {
        val rejectReason = "페이스 이미지에 얼굴이 제대로 확인되지 않습니다."

        adminService.rejectMemberProfile(memberPending.id!!, rejectReason)

        val findMember = memberService.findMember(memberId = memberPending.id!!)

        assertAll(
            { assertThat(findMember.memberStatus).isEqualTo(MemberStatus.REJECT) },
            { assertThat(findMember.rejectReason).isEqualTo(rejectReason) },
        )
    }
}
