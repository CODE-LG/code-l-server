package codel.admin.business

import codel.admin.domain.Admin
import codel.admin.presentation.request.ImageRejection
import codel.auth.business.AuthService
import codel.config.Loggable
import codel.member.business.MemberService
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.RejectionHistory
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.question.business.QuestionService
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import codel.verification.domain.StandardVerificationImage
import codel.verification.domain.VerificationImage
import codel.verification.infrastructure.StandardVerificationImageJpaRepository
import codel.verification.infrastructure.VerificationImageJpaRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@Service
@Transactional(readOnly = true)
class AdminService(
    private val memberService: MemberService,
    private val authService: AuthService,
    private val asyncNotificationService: IAsyncNotificationService,
    private val questionService: QuestionService,
    private val standardVerificationImageRepository: StandardVerificationImageJpaRepository,
    private val verificationImageRepository: VerificationImageJpaRepository,
    private val imageUploader: ImageUploader,
    @Value("\${security.admin.password}")
    private val answerPassword: String,
) : Loggable{
    @Transactional
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
    
    /**
     * 관리자용: 이미지 포함해서 회원 조회
     */
    fun findMemberWithImages(memberId: Long): Member = memberService.findMemberWithImages(memberId)

    @Transactional
    fun approveMemberProfile(memberId: Long) {
        val approvedMember = memberService.approveMember(memberId)
        sendApprovalNotification(approvedMember)
    }

    @Transactional
    fun rejectMemberProfile(
        memberId: Long,
        reason: String,
    ) {
        val rejectedMember = memberService.rejectMember(memberId, reason)
        sendRejectionNotification(rejectedMember)
    }
    
    /**
     * 이미지별 거절 처리 (신규)
     */
    @Transactional
    fun rejectMemberProfileWithImages(
        memberId: Long,
        faceImageRejections: List<ImageRejection>?,
        codeImageRejections: List<ImageRejection>?
    ) {
        val rejectedMember = memberService.rejectMemberWithImages(memberId, faceImageRejections, codeImageRejections)
        sendRejectionNotification(rejectedMember)
    }

    // ========== 알림 전송 메서드 ==========
    
    /**
     * 승인 알림 전송 (FCM + Discord)
     */
    private fun sendApprovalNotification(member: Member) {
        // 1. FCM 알림 전송
        sendApprovalFcmNotification(member)
        
        // 2. Discord 알림 전송
        sendApprovalDiscordNotification(member)
    }
    
    /**
     * 반려 알림 전송 (FCM + Discord)
     */
    private fun sendRejectionNotification(member: Member) {
        // 1. FCM 알림 전송
        sendRejectionFcmNotification(member)
        
        // 2. Discord 알림 전송
        sendRejectionDiscordNotification(member)
    }

    /**
     * 승인 FCM 알림
     */
    private fun sendApprovalFcmNotification(member: Member) {
        member.fcmToken?.let { token ->
            val notification = Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = "프로필 심사가 완료되었어요 ✅",
                body = "이제 Code:L을 이용할 수 있어요. 코드가 맞는 우리만의 공간에서 진짜 인연을 만나보세요."
            )
            
            // 비동기 알림 전송으로 변경
            asyncNotificationService.sendAsync(notification)
                .thenAccept { result ->
                    if (result.success) {
                        log.info { "✅ 프로필 승인 알림 전송 성공 - 회원: ${member.getIdOrThrow()}" }
                    } else {
                        log.warn { "❌ 프로필 승인 알림 전송 실패 - 회원: ${member.getIdOrThrow()}, 사유: ${result.error}" }
                    }
                }
                .exceptionally { e ->
                    log.warn(e) { "❌ 프로필 승인 알림 전송 예외 발생 - 회원: ${member.getIdOrThrow()}" }
                    null
                }
        } ?: run {
            log.info { "ℹ️ FCM 토큰이 없어 프로필 승인 알림을 전송하지 않음 - 회원: ${member.getIdOrThrow()}" }
        }
    }

    /**
     * 반려 FCM 알림
     */
    private fun sendRejectionFcmNotification(member: Member) {
        member.fcmToken?.let { token ->
            val notification = Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = "프로필 심사가 반려되었습니다 ❌",
                body = "자세한 이유는 앱에서 확인할 수 있습니다."
            )
            
            // 비동기 알림 전송으로 변경
            asyncNotificationService.sendAsync(notification)
                .thenAccept { result ->
                    if (result.success) {
                        log.info { "✅ 프로필 반려 알림 전송 성공 - 회원: ${member.getIdOrThrow()}" }
                    } else {
                        log.warn { "❌ 프로필 반려 알림 전송 실패 - 회원: ${member.getIdOrThrow()}, 사유: ${result.error}" }
                    }
                }
                .exceptionally { e ->
                    log.warn(e) { "❌ 프로필 반려 알림 전송 예외 발생 - 회원: ${member.getIdOrThrow()}" }
                    null
                }
        } ?: run {
            log.info { "ℹ️ FCM 토큰이 없어 프로필 반려 알림을 전송하지 않음 - 회원: ${member.getIdOrThrow()}" }
        }
    }

    /**
     * 승인 Discord 알림
     */
    private fun sendApprovalDiscordNotification(member: Member) {
        try {
            val notification = Notification(
                type = NotificationType.DISCORD,
                targetId = member.getIdOrThrow().toString(),
                title = "✅ 프로필 승인 완료",
                body = """
                    **회원 프로필이 승인되었습니다.**
                    
                    👤 **회원 정보**
                    • 코드네임: **${member.getProfileOrThrow().getCodeNameOrThrow()}**
                    • 회원 ID: ${member.getIdOrThrow()}
                    
                    📱 **알림 전송**
                    • FCM 알림: ${if (member.fcmToken != null) "전송 완료 ✅" else "토큰 없음 ⚠️"}
                    
                    🕐 **처리 시각**
                    • ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} (KST)
                """.trimIndent()
            )
            
            // Discord는 동기 전송 유지 (관리자용이므로)
            asyncNotificationService.sendAsync(notification)
                .thenAccept { result ->
                    if (result.success) {
                        log.info { "✅ Discord 승인 알림 전송 완료 - 회원: ${member.getIdOrThrow()}" }
                    } else {
                        log.warn { "❌ Discord 승인 알림 전송 실패 - 회원: ${member.getIdOrThrow()}" }
                    }
                }
        } catch (e: Exception) {
            log.warn(e) { "❌ Discord 승인 알림 전송 실패 - 회원: ${member.getIdOrThrow()}" }
        }
    }

    /**
     * 반려 Discord 알림
     */
    private fun sendRejectionDiscordNotification(member: Member) {
        try {
            val rejectReason = member.rejectReason ?: "사유 없음"
            
            val notification = Notification(
                type = NotificationType.DISCORD,
                targetId = member.getIdOrThrow().toString(),
                title = "❌ 프로필 반려 처리",
                body = """
                    **회원 프로필이 반려되었습니다.**
                    
                    👤 **회원 정보**
                    • 코드네임: **${member.getProfileOrThrow().getCodeNameOrThrow()}**
                    • 회원 ID: ${member.getIdOrThrow()}
                    
                    📝 **반려 사유**
                    • $rejectReason
                    
                    📱 **알림 전송**
                    • FCM 알림: ${if (member.fcmToken != null) "전송 완료 ✅" else "토큰 없음 ⚠️"}
                    
                    🕐 **처리 시각**
                    • ${java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))} (KST)
                """.trimIndent()
            )
            
            // Discord는 동기 전송 유지 (관리자용이므로)
            asyncNotificationService.sendAsync(notification)
                .thenAccept { result ->
                    if (result.success) {
                        log.info { "✅ Discord 반려 알림 전송 완료 - 회원: ${member.getIdOrThrow()}" }
                    } else {
                        log.warn { "❌ Discord 반려 알림 전송 실패 - 회원: ${member.getIdOrThrow()}" }
                    }
                }
        } catch (e: Exception) {
            log.warn(e) { "❌ Discord 반려 알림 전송 실패 - 회원: ${member.getIdOrThrow()}" }
        }
    }


    fun countAllMembers(): Long = memberService.countAllMembers()

    fun countPendingMembers(): Long = memberService.countPendingMembers()

    fun findMembersWithFilter(
        keyword: String?,
        status: String?,
        pageable: Pageable,
    ): Page<Member> = memberService.findMembersWithFilter(keyword, status, pageable)
    
    fun findMembersWithFilter(
        keyword: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        sort: String?,
        direction: String?,
        pageable: Pageable,
    ): Page<Member> = memberService.findMembersWithFilter(keyword, status, startDate, endDate, sort, direction, pageable)
    
    fun countMembersByStatus(status: String): Long = memberService.countMembersByStatus(status)

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

    // ========== 거절 이력 관리 ==========
    
    /**
     * 특정 회원의 모든 거절 이력 조회
     */
    fun getRejectionHistories(memberId: Long): List<RejectionHistory> {
        return memberService.getRejectionHistories(memberId)
    }

    /**
     * 특정 회원의 특정 차수 거절 이력 조회
     */
    fun getRejectionHistoriesByRound(memberId: Long, rejectionRound: Int): List<RejectionHistory> {
        return memberService.getRejectionHistoriesByRound(memberId, rejectionRound)
    }

    /**
     * 특정 회원의 최대 거절 차수 조회
     */
    fun getMaxRejectionRound(memberId: Long): Int {
        return memberService.getMaxRejectionRound(memberId)
    }

    // ===== 표준 인증 이미지 관리 =====

    /**
     * 모든 표준 인증 이미지 조회 (관리자용)
     */
    fun getAllStandardVerificationImages(): List<StandardVerificationImage> {
        return standardVerificationImageRepository.findAll().sortedByDescending { it.createdAt }
    }

    /**
     * 활성화된 표준 인증 이미지 조회
     */
    fun getActiveStandardVerificationImages(): List<StandardVerificationImage> {
        return standardVerificationImageRepository.findAllByIsActiveTrue().sortedByDescending { it.createdAt }
    }

    /**
     * 표준 인증 이미지 생성
     */
    @Transactional
    fun createStandardVerificationImage(
        imageFile: MultipartFile,
        description: String?
    ): StandardVerificationImage {
        // S3에 이미지 업로드
        val imageUrl = imageUploader.uploadFile(imageFile)

        // 표준 인증 이미지 엔티티 생성
        val standardImage = StandardVerificationImage(
            imageUrl = imageUrl,
            description = description ?: "",
            isActive = true
        )

        return standardVerificationImageRepository.save(standardImage)
    }

    /**
     * 표준 인증 이미지 활성화/비활성화 토글
     */
    @Transactional
    fun toggleStandardImageStatus(imageId: Long): StandardVerificationImage {
        val image = standardVerificationImageRepository.findById(imageId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "표준 인증 이미지를 찾을 수 없습니다. ID: $imageId")
        }
        image.isActive = !image.isActive
        return image
    }

    /**
     * 표준 인증 이미지 삭제
     */
    @Transactional
    fun deleteStandardVerificationImage(imageId: Long) {
        val image = standardVerificationImageRepository.findById(imageId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "표준 인증 이미지를 찾을 수 없습니다. ID: $imageId")
        }
        standardVerificationImageRepository.delete(image)
        // S3에서 이미지 삭제는 별도 처리 필요 (비동기 권장)
    }

    /**
     * 회원의 최신 인증 이미지 조회 (있는 경우만)
     * standardVerificationImage를 함께 fetch
     */
    fun getMemberVerificationImage(member: Member): VerificationImage? {
        return verificationImageRepository.findFirstByMemberWithStandardImage(member)
    }
}
