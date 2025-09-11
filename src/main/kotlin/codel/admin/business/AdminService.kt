package codel.admin.business

import codel.admin.domain.Admin
import codel.auth.business.AuthService
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.question.business.QuestionService
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class AdminService(
    private val memberService: MemberService,
    private val authService: AuthService,
    private val notificationService: NotificationService,
    private val questionService: QuestionService,
    @Value("\${security.admin.password}")
    private val answerPassword: String,
) {
    fun loginAdmin(admin: Admin): String {
        admin.validatePassword(answerPassword)

        val member =
            Member(
                oauthType = admin.oauthType,
                oauthId = admin.oauthId,
                memberStatus = admin.memberStatus,
                email = "hogee",
            )

        memberService.loginMember(member)

        return authService.provideToken(member)
    }

    fun findPendingMembers(): List<Member> = memberService.findPendingMembers()

    fun findMember(memberId: Long): Member = memberService.findMember(memberId)

    @Transactional
    fun approveMemberProfile(memberId: Long) {
        val approvedMember = memberService.approveMember(memberId)
//        sendNotification(approvedMember)
    }

    @Transactional
    fun rejectMemberProfile(
        memberId: Long,
        reason: String,
    ) {
        val rejectedMember = memberService.rejectMember(memberId, reason)
//        sendNotification(rejectedMember)
    }

    fun countAllMembers(): Long = memberService.countAllMembers()

    fun countPendingMembers(): Long = memberService.countPendingMembers()

    private fun sendNotification(member: Member) {
        member.fcmToken?.let { fcmToken ->
            notificationService.send(
                notification =
                    Notification(
                        type = NotificationType.MOBILE,
                        targetId = fcmToken,
                        title = "심사가 완료되었습니다.",
                        body = "code:L 프로필 심사가 완료되었습니다.",
                    ),
            )
        }
    }

    fun findMembersWithFilter(
        keyword: String?,
        status: String?,
        pageable: Pageable,
    ): Page<Member> = memberService.findMembersWithFilter(keyword, status, pageable)

    // ========== 질문 관리 ==========
    
    fun findQuestionsWithFilter(
        keyword: String?,
        category: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<Question> = questionService.findQuestionsWithFilter(keyword, category, isActive, pageable)
    
    fun findQuestionById(questionId: Long): Question = questionService.findQuestionById(questionId)
    
    @Transactional
    fun createQuestion(
        content: String,
        category: QuestionCategory,
        description: String?,
        isActive: Boolean
    ): Question = questionService.createQuestion(content, category, description, isActive)
    
    @Transactional
    fun updateQuestion(
        questionId: Long,
        content: String,
        category: QuestionCategory,
        description: String?,
        isActive: Boolean
    ): Question = questionService.updateQuestion(questionId, content, category, description, isActive)
    
    @Transactional
    fun deleteQuestion(questionId: Long) = questionService.deleteQuestion(questionId)
    
    @Transactional
    fun toggleQuestionStatus(questionId: Long): Question = questionService.toggleQuestionStatus(questionId)

    // ========== 통계 관련 메서드 ==========
    
    fun getDailySignupStats(): List<Pair<String, Long>> = memberService.getDailySignupStats()
    
    fun getMemberStatusStats(): Map<String, Long> = memberService.getMemberStatusStats()
    
    fun getMonthlySignupStats(): List<Triple<Int, Int, Long>> = memberService.getMonthlySignupStats()
    
    fun getTodaySignupCount(): Long = memberService.getTodaySignupCount()
    
    fun getWeeklySignupCount(): Long = memberService.getWeeklySignupCount()
    
    fun getMonthlySignupCount(): Long = memberService.getMonthlySignupCount()
    
    fun getApprovalRate(): Double {
        val statusStats = getMemberStatusStats()
        val doneCount = statusStats["DONE"] ?: 0L
        val pendingCount = statusStats["PENDING"] ?: 0L
        val rejectCount = statusStats["REJECT"] ?: 0L
        
        val totalProcessed = doneCount + rejectCount
        return if (totalProcessed > 0) {
            (doneCount.toDouble() / totalProcessed.toDouble()) * 100
        } else {
            0.0
        }
    }

    // ========== 프로필 관련 추가 메서드들 ==========
    
    // 회원 활동 히스토리 조회 (임시 구현)
    fun getMemberActivityHistory(memberId: Long): List<MemberActivity> {
        // 실제 구현에서는 활동 기록을 저장하는 테이블에서 조회
        return emptyList()
    }
    
    // 회원 상태 변경 히스토리 조회 (임시 구현)
    fun getMemberStatusHistory(memberId: Long): List<MemberStatusHistory> {
        // 실제 구현에서는 상태 변경 이력을 저장하는 테이블에서 조회
        return emptyList()
    }
    
    // 회원 로그인 히스토리 조회 (임시 구현)
    fun getMemberLoginHistory(memberId: Long, limit: Int): List<MemberLoginHistory> {
        // 실제 구현에서는 로그인 기록을 저장하는 테이블에서 조회
        return emptyList()
    }
    
    // 회원 신고 히스토리 조회 (임시 구현)
    fun getMemberReportHistory(memberId: Long): List<MemberReport> {
        // 실제 구현에서는 신고 기록을 저장하는 테이블에서 조회
        return emptyList()
    }
    
    // 관리자 메모 조회 (임시 구현)
    fun getAdminNotes(memberId: Long): List<AdminNote> {
        // 실제 구현에서는 관리자 메모를 저장하는 테이블에서 조회
        return emptyList()
    }
    
    // 최근 회원 활동 조회 (임시 구현)
    fun getRecentMemberActivity(memberId: Long, limit: Int): List<MemberActivity> {
        // 실제 구현에서는 최근 활동을 조회
        return emptyList()
    }
    
    // 회원 통계 조회 (임시 구현)
    fun getMemberStatistics(memberId: Long): MemberStatistics? {
        // 실제 구현에서는 회원별 통계를 계산
        return null
    }

    // ========== 데이터 클래스들 ==========
    
    data class MemberActivity(
        val id: Long?,
        val memberId: Long,
        val type: String,
        val description: String,
        val createdAt: java.time.LocalDateTime
    )
    
    data class MemberStatusHistory(
        val id: Long?,
        val memberId: Long,
        val fromStatus: String,
        val toStatus: String,
        val reason: String?,
        val createdAt: java.time.LocalDateTime
    )
    
    data class MemberLoginHistory(
        val id: Long?,
        val memberId: Long,
        val loginAt: java.time.LocalDateTime,
        val ipAddress: String?,
        val userAgent: String?
    )
    
    data class MemberReport(
        val id: Long?,
        val memberId: Long,
        val reporterName: String,
        val reason: String,
        val createdAt: java.time.LocalDateTime
    )
    
    data class AdminNote(
        val id: Long?,
        val memberId: Long,
        val adminName: String,
        val content: String,
        val createdAt: java.time.LocalDateTime
    )
    
    data class MemberStatistics(
        val loginCount: Int,
        val activityScore: Double,
        val reportCount: Int,
        val daysSinceJoin: Int,
        val profileCompletionRate: Double,
        val lastActiveDate: java.time.LocalDate?,
        val totalImages: Int,
        val hasIntroduce: Boolean
    )
}
