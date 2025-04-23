package codel.admin.business

import codel.admin.domain.Admin
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AdminService(
    private val memberService: MemberService,
    private val notificationService: NotificationService,
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

    fun findPendingMembers(): List<Member> = memberService.findPendingMembers()

    fun approveMemberProfile(targetId: Long) {
        val approvedMember = memberService.approveMember(targetId)
        sendNotification(approvedMember)
    }

    fun rejectMemberProfile(
        targetId: Long,
        reason: String,
    ) {
        val rejectedMember = memberService.rejectMember(targetId, reason)
        sendNotification(rejectedMember)
    }

    private fun sendNotification(member: Member) {
        member.fcmToken?.let { fcmToken ->
            notificationService.sendPushNotification(
                notification =
                    Notification(
                        token = fcmToken,
                        title = "심사가 완료되었습니다.",
                        body = "code:L 프로필 심사가 완료되었습니다.",
                    ),
            )
        }
    }
}
