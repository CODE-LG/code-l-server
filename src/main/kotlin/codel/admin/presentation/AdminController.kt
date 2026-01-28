package codel.admin.presentation

import codel.admin.business.AdminService
import codel.report.business.ReportAdminService
import codel.report.domain.ReportStatus
import codel.admin.domain.Admin
import codel.admin.exception.AdminException
import codel.admin.presentation.request.AdminLoginRequest
import codel.admin.presentation.request.RejectProfileRequest
import codel.member.domain.Member
import codel.question.domain.QuestionCategory
import codel.question.domain.QuestionGroup
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AdminController(
    private val adminService: AdminService,
    private val reportAdminService: ReportAdminService,
) {
    @GetMapping("/v1/admin/login")
    fun login(): String = "login"

    @PostMapping("/v1/admin/login")
    fun login(
        @ModelAttribute adminLoginRequest: AdminLoginRequest,
        response: HttpServletResponse,
        model: Model,
    ): String {
        val admin = Admin(adminLoginRequest.password)

        return try {
            val token = adminService.loginAdmin(admin)
            addCookie(token, response)
            "redirect:/v1/admin/home"
        } catch (e: AdminException) {
            model.addAttribute("error", e.message)
            "login"
        }
    }

    private fun addCookie(
        token: String,
        response: HttpServletResponse,
    ) {
        val cookie = Cookie("access_token", token)
        cookie.path = "/v1/admin"
        cookie.maxAge = 86400

        response.addCookie(cookie)
    }

    @GetMapping("/v1/admin/home")
    fun home(model: Model): String {
        // 기본 통계
        val totalMembers = adminService.countAllMembers()
        val pendingMembers = adminService.countPendingMembers()
        val todaySignups = adminService.getTodaySignupCount()
        val weeklySignups = adminService.getWeeklySignupCount()
        val monthlySignups = adminService.getMonthlySignupCount()
        val approvalRate = adminService.getApprovalRate()

        // 상태별 통계
        val statusStats = adminService.getMemberStatusStats()

        // 일별 가입자 통계 (차트용)
        val dailyStats = adminService.getDailySignupStats()

        // 월별 가입자 통계
        val monthlyStats = adminService.getMonthlySignupStats()

        model.addAttribute("totalMembers", totalMembers)
        model.addAttribute("pendingMembers", pendingMembers)
        model.addAttribute("todaySignups", todaySignups)
        model.addAttribute("weeklySignups", weeklySignups)
        model.addAttribute("monthlySignups", monthlySignups)
        model.addAttribute("approvalRate", String.format("%.1f", approvalRate))
        model.addAttribute("statusStats", statusStats)
        model.addAttribute("dailyStats", dailyStats)
        model.addAttribute("monthlyStats", monthlyStats)

        return "home"
    }

    @GetMapping("/v1/admin/member/{memberId}")
    fun findMemberDetail(
        model: Model,
        @PathVariable memberId: Long,
    ): String {
        println("🔍 AdminController.findMemberDetail 호출됨 - memberId: $memberId")

        try {
            // 기본 회원 정보 조회 (이미지 포함)
            println("📄 회원 정보 조회 시작")
            val member = adminService.findMemberWithImages(memberId)
            println("✅ 회원 정보 조회 성공: ${member.email}")

            // 프로필 정보 안전하게 가져오기
            val profile = member.profile
            println("📋 프로필 정보: ${if (profile != null) "존재함" else "없음"}")

            // 이미지 리스트 안전하게 가져오기 (ID와 URL 포함)
            val codeImages = try {
                profile?.codeImages?.sortedBy { it.orders }?.map {
                    mapOf("id" to it.id, "url" to it.url, "isApproved" to it.isApproved, "rejectionReason" to it.rejectionReason)
                } ?: emptyList()
            } catch (e: Exception) {
                println("⚠️ Error getting code images: ${e.message}")
                emptyList<Map<String, Any?>>()
            }

            val faceImages = try {
                profile?.faceImages?.sortedBy { it.orders }?.map {
                    mapOf("id" to it.id, "url" to it.url, "isApproved" to it.isApproved, "rejectionReason" to it.rejectionReason)
                } ?: emptyList()
            } catch (e: Exception) {
                println("⚠️ Error getting face images: ${e.message}")
                emptyList<Map<String, Any?>>()
            }

            println("🖼️ 이미지 정보 - 코드: ${codeImages.size}개, 페이스: ${faceImages.size}개")

        // 추가 정보들 조회 (옵셔널)
        val activityHistory = try {
            // adminService.getMemberActivityHistory(memberId)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        val statusHistory = try {
            // adminService.getMemberStatusHistory(memberId)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        val loginHistory = try {
            // adminService.getMemberLoginHistory(memberId, 10)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        val reportHistory = try {
            // adminService.getMemberReportHistory(memberId)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        val adminNotes = try {
            // adminService.getAdminNotes(memberId)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        val recentActivity = try {
            // adminService.getRecentMemberActivity(memberId, 5)
            emptyList<Any>() // 명시적 타입 지정
        } catch (e: Exception) {
            emptyList<Any>()
        }

        // 회원 통계 정보 (옵셔널)
        val memberStats = try {
            // adminService.getMemberStatistics(memberId)
            null // 임시로 null
        } catch (e: Exception) {
            null
        }

        val repQuestionContent = try {
            profile?.representativeQuestion?.content
        } catch (e: Exception) {
            println("⚠️ Error getting representative question: ${e.message}")
            null
        }

        // 인증 이미지 조회 (없을 수도 있음)
        val verificationImage = try {
            adminService.getMemberVerificationImage(member)
        } catch (e: Exception) {
            println("⚠️ Error getting verification image: ${e.message}")
            null
        }

        // 모델에 모든 데이터 추가
        model.addAttribute("member", member)
        model.addAttribute("codeImages", codeImages)
        model.addAttribute("faceImages", faceImages)
        model.addAttribute("verificationImage", verificationImage)
        model.addAttribute("activityHistory", activityHistory)
        model.addAttribute("statusHistory", statusHistory)
        model.addAttribute("loginHistory", loginHistory)
        model.addAttribute("repQuestionContent", repQuestionContent)
        model.addAttribute("reportHistory", reportHistory)
        model.addAttribute("adminNotes", adminNotes)
        model.addAttribute("recentActivity", recentActivity)
        model.addAttribute("memberStats", memberStats)

        return "memberDetail"

        } catch (e: Exception) {
            println("Error in findMemberDetail for memberId $memberId: ${e.message}")
            e.printStackTrace()

            // 오류 발생 시 회원 목록으로 리다이렉트
            model.addAttribute("error", "회원 정보를 불러올 수 없습니다 (ID: $memberId)")
            return "redirect:/v1/admin/members"
        }
    }

    /**
     * 이미지 심사 전용 페이지
     */
    @GetMapping("/v1/admin/member/{memberId}/image-review")
    fun memberImageReview(
        model: Model,
        @PathVariable memberId: Long
    ): String {
        try {
            val member = adminService.findMemberWithImages(memberId)
            val profile = member.profile

            // 이미지 리스트 가져오기
            val codeImages = profile?.codeImages?.sortedBy { it.orders }?.map {
                mapOf("id" to it.id, "url" to it.url)
            } ?: emptyList()

            val faceImages = profile?.faceImages?.sortedBy { it.orders }?.map {
                mapOf("id" to it.id, "url" to it.url)
            } ?: emptyList()

            // 인증 이미지 조회 (없을 수도 있음)
            val verificationImage = try {
                adminService.getMemberVerificationImage(member)
            } catch (e: Exception) {
                null
            }

            // 표준 이미지 정보 (인증 이미지가 있는 경우)
            val standardImage = verificationImage?.standardVerificationImage

            model.addAttribute("member", member)
            model.addAttribute("codeImages", codeImages)
            model.addAttribute("faceImages", faceImages)
            model.addAttribute("verificationImage", verificationImage)
            model.addAttribute("standardImage", standardImage)

            return "memberImageReview"
        } catch (e: Exception) {
            e.printStackTrace()
            model.addAttribute("error", "이미지를 불러올 수 없습니다")
            return "redirect:/v1/admin/member/$memberId"
        }
    }

    @PostMapping("/v1/admin/approval/{memberId}")
    fun approveMember(
        @PathVariable memberId: Long,
    ): String {
        adminService.approveMemberProfile(memberId)

        return "redirect:/v1/admin/home"
    }

    @PostMapping("/v1/admin/reject/{memberId}")
    fun rejectMember(
        @PathVariable memberId: Long,
        @RequestParam rejectReason: String,
    ): String {
        adminService.rejectMemberProfile(memberId, rejectReason)

        return "redirect:/v1/admin/home"
    }

    /**
     * 이미지별 거절 처리 API (신규)
     */
    @PostMapping("/v1/admin/reject-images/{memberId}")
    @ResponseBody
    fun rejectMemberWithImages(
        @PathVariable memberId: Long,
        @RequestBody request: RejectProfileRequest
    ): ResponseEntity<Map<String, String>> {
        adminService.rejectMemberProfileWithImages(
            memberId,
            request.faceImageRejections,
            request.codeImageRejections
        )

        return ResponseEntity.ok(mapOf("message" to "프로필 거절 처리가 완료되었습니다"))
    }

    @GetMapping("/v1/admin/members")
    fun memberList(
        model: Model,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @RequestParam(required = false, defaultValue = "createdAt") sort: String?,
        @RequestParam(required = false, defaultValue = "desc") direction: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): String {
        val members: Page<Member> = adminService.findMembersWithFilter(keyword, status, startDate, endDate, sort, direction, pageable)

        // 각 상태별 회원 수 조회
        val statusCounts = mapOf(
            "total" to adminService.countAllMembers(),
            "PENDING" to adminService.countMembersByStatus("PENDING"),
            "DONE" to adminService.countMembersByStatus("DONE"),
            "REJECT" to adminService.countMembersByStatus("REJECT"),
            "PHONE_VERIFIED" to adminService.countMembersByStatus("PHONE_VERIFIED")
        )

        model.addAttribute("members", members)
        model.addAttribute("statusCounts", statusCounts)
        model.addAttribute("param", mapOf(
            "keyword" to (keyword ?: ""),
            "status" to (status ?: ""),
            "startDate" to (startDate ?: ""),
            "endDate" to (endDate ?: ""),
            "sort" to (sort ?: "createdAt"),
            "direction" to (direction ?: "desc")
        ))
        return "memberList"
    }

    @PostMapping("/v1/admin/members/bulk-action")
    fun bulkAction(
        @RequestParam action: String,
        @RequestParam memberIds: List<Long>,
        @RequestParam(required = false) rejectReason: String?
    ): String {
        when (action) {
            "approve" -> memberIds.forEach { adminService.approveMemberProfile(it) }
            "reject" -> {
                val reason = rejectReason ?: "일괄 거부"
                memberIds.forEach { adminService.rejectMemberProfile(it, reason) }
            }
        }
        return "redirect:/v1/admin/members"
    }

    // ========== 질문 관리 ==========

    @GetMapping("/v1/admin/questions")
    fun questionList(
        model: Model,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) purpose: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) questionGroup: String?,
        @RequestParam(required = false) isActive: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): String {
        val questions = adminService.findQuestionsWithFilterV2(keyword, category, questionGroup, isActive, pageable)
        model.addAttribute("questions", questions)
        model.addAttribute("categories", QuestionCategory.values())
        model.addAttribute("questionGroups", QuestionGroup.values())
        model.addAttribute("searchParams", mapOf(
            "keyword" to (keyword ?: ""),
            "purpose" to (purpose ?: ""),
            "category" to (category ?: ""),
            "questionGroup" to (questionGroup ?: ""),
            "isActive" to (isActive?.toString() ?: "")
        ))
        return "questionList"
    }

    @GetMapping("/v1/admin/questions/new")
    fun questionForm(model: Model): String {
        model.addAttribute("categories", QuestionCategory.values())
        model.addAttribute("questionGroups", QuestionGroup.values())
        return "questionForm"
    }

    @PostMapping("/v1/admin/questions")
    fun createQuestion(
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam questionGroup: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(defaultValue = "true") isActive: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val questionCategory = QuestionCategory.valueOf(category)
            val group = QuestionGroup.valueOf(questionGroup)
            adminService.createQuestionV2(content, questionCategory, group, description, isActive)
            redirectAttributes.addFlashAttribute("success", "질문이 성공적으로 등록되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "질문 등록에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/questions"
    }

    @GetMapping("/v1/admin/questions/{questionId}/edit")
    fun editQuestionForm(
        @PathVariable questionId: Long,
        model: Model
    ): String {
        val question = adminService.findQuestionById(questionId)
        model.addAttribute("question", question)
        model.addAttribute("categories", QuestionCategory.values())
        model.addAttribute("questionGroups", QuestionGroup.values())
        return "questionEditForm"
    }

    @PostMapping("/v1/admin/questions/{questionId}")
    fun updateQuestion(
        @PathVariable questionId: Long,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam questionGroup: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(defaultValue = "false") isActive: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val questionCategory = QuestionCategory.valueOf(category)
            val group = QuestionGroup.valueOf(questionGroup)
            adminService.updateQuestionV2(questionId, content, questionCategory, group, description, isActive)
            redirectAttributes.addFlashAttribute("success", "질문이 성공적으로 수정되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "질문 수정에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/questions"
    }

    @PostMapping("/v1/admin/questions/{questionId}/delete")
    fun deleteQuestion(
        @PathVariable questionId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            adminService.deleteQuestion(questionId)
            redirectAttributes.addFlashAttribute("success", "질문이 성공적으로 삭제되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "질문 삭제에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/questions"
    }

    @PostMapping("/v1/admin/questions/{questionId}/toggle")
    fun toggleQuestionStatus(
        @PathVariable questionId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val question = adminService.toggleQuestionStatus(questionId)
            val status = if (question.isActive) "활성화" else "비활성화"
            redirectAttributes.addFlashAttribute("success", "질문이 성공적으로 ${status}되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "질문 상태 변경에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/questions"
    }

    // ========== 신고 관리 ==========

    /**
     * 신고 목록 조회 페이지
     */
    @GetMapping("/v1/admin/reports")
    fun reportList(
        model: Model,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) startDate: String?,
        @RequestParam(required = false) endDate: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): String {
        // 신고 목록 조회
        val reports = reportAdminService.getReportsWithFilter(keyword, status, startDate, endDate, pageable)

        // 통계 정보
        val stats = mapOf(
            "total" to reportAdminService.getTotalReportsCount(),
            "pending" to reportAdminService.getReportCountByStatus(ReportStatus.PENDING),
            "inProgress" to reportAdminService.getReportCountByStatus(ReportStatus.IN_PROGRESS),
            "resolved" to reportAdminService.getReportCountByStatus(ReportStatus.RESOLVED),
            "dismissed" to reportAdminService.getReportCountByStatus(ReportStatus.DISMISSED),
            "today" to reportAdminService.getTodayReportsCount(),
            "weekly" to reportAdminService.getWeeklyReportsCount(),
            "monthly" to reportAdminService.getMonthlyReportsCount()
        )

        // 신고 많이 받은 사용자 TOP 10
        val topReported = reportAdminService.getMostReportedMembers(30, 10)

        model.addAttribute("reports", reports)
        model.addAttribute("stats", stats)
        model.addAttribute("topReported", topReported)
        model.addAttribute("statuses", ReportStatus.values())
        model.addAttribute("param", mapOf(
            "keyword" to (keyword ?: ""),
            "status" to (status ?: ""),
            "startDate" to (startDate ?: ""),
            "endDate" to (endDate ?: "")
        ))

        return "reportList"
    }

    /**
     * 신고 상세 조회 페이지
     */
    @GetMapping("/v1/admin/reports/{reportId}")
    fun reportDetail(
        model: Model,
        @PathVariable reportId: Long
    ): String {
        try {
            val report = reportAdminService.getReportDetail(reportId)

            // 피신고자의 신고 이력
            val reportedMemberId = report.reported.getIdOrThrow()
            val reportHistory = reportAdminService.getReportedMemberReports(
                reportedMemberId,
                Pageable.ofSize(10)
            )
            val totalReportCount = reportAdminService.getReportedMemberReportCount(reportedMemberId)

            model.addAttribute("report", report)
            model.addAttribute("reportHistory", reportHistory)
            model.addAttribute("totalReportCount", totalReportCount)
            model.addAttribute("statuses", ReportStatus.values())

            return "reportDetail"
        } catch (e: Exception) {
            model.addAttribute("error", "신고 내역을 불러올 수 없습니다 (ID: $reportId)")
            return "redirect:/v1/admin/reports"
        }
    }

    /**
     * 신고 처리 상태 변경
     */
    @PostMapping("/v1/admin/reports/{reportId}/status")
    fun updateReportStatus(
        @PathVariable reportId: Long,
        @RequestParam status: String,
        @RequestParam(required = false) note: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val reportStatus = ReportStatus.valueOf(status)
            reportAdminService.updateReportStatus(reportId, reportStatus, note)
            redirectAttributes.addFlashAttribute("success", "신고 상태가 변경되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "신고 상태 변경에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/reports/$reportId"
    }

    /**
     * 신고 처리 완료
     */
    @PostMapping("/v1/admin/reports/{reportId}/resolve")
    fun resolveReport(
        @PathVariable reportId: Long,
        @RequestParam(required = false) note: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            reportAdminService.resolveReport(reportId, note)
            redirectAttributes.addFlashAttribute("success", "신고가 처리되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "신고 처리에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/reports/$reportId"
    }

    /**
     * 신고 반려
     */
    @PostMapping("/v1/admin/reports/{reportId}/dismiss")
    fun dismissReport(
        @PathVariable reportId: Long,
        @RequestParam(required = false) note: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            reportAdminService.dismissReport(reportId, note)
            redirectAttributes.addFlashAttribute("success", "신고가 반려되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "신고 반려에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/reports/$reportId"
    }

    /**
     * 회원의 거절 이력 조회 (API)
     */
    @GetMapping("/v1/admin/members/{memberId}/rejection-histories")
    @ResponseBody
    fun getRejectionHistories(
        @PathVariable memberId: Long
    ): ResponseEntity<Map<String, Any>> {
        val member = adminService.findMember(memberId)
        val histories = adminService.getRejectionHistories(memberId)
        val maxRound = if (histories.isEmpty()) 0 else adminService.getMaxRejectionRound(memberId)

        val historyResponses = histories.map { history ->
            codel.admin.presentation.response.RejectionHistoryResponse.from(history)
        }

        val response = mapOf(
            "memberId" to memberId,
            "memberName" to (member.profile?.codeName ?: "이름 없음"),
            "totalRejectionCount" to histories.size,
            "maxRejectionRound" to maxRound,
            "histories" to historyResponses
        )

        return ResponseEntity.ok(response)
    }

    // ========== 표준 인증 이미지 관리 ==========

    /**
     * 표준 인증 이미지 목록 페이지
     */
    @GetMapping("/v1/admin/verification-images")
    fun standardImageList(model: Model): String {
        val images = adminService.getAllStandardVerificationImages()
        model.addAttribute("images", images)
        return "verificationImageList"
    }

    /**
     * 표준 인증 이미지 등록 페이지
     */
    @GetMapping("/v1/admin/verification-images/new")
    fun standardImageForm(model: Model): String {
        return "verificationImageForm"
    }

    /**
     * 표준 인증 이미지 등록 처리
     */
    @PostMapping("/v1/admin/verification-images")
    fun createStandardImage(
        @RequestParam imageFile: org.springframework.web.multipart.MultipartFile,
        @RequestParam(required = false) description: String?,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            adminService.createStandardVerificationImage(imageFile, description)
            redirectAttributes.addFlashAttribute("success", "표준 인증 이미지가 성공적으로 등록되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "이미지 등록에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/verification-images"
    }

    /**
     * 표준 인증 이미지 활성화/비활성화 토글
     */
    @PostMapping("/v1/admin/verification-images/{imageId}/toggle")
    fun toggleStandardImageStatus(
        @PathVariable imageId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val image = adminService.toggleStandardImageStatus(imageId)
            val status = if (image.isActive) "활성화" else "비활성화"
            redirectAttributes.addFlashAttribute("success", "표준 인증 이미지가 성공적으로 ${status}되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "상태 변경에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/verification-images"
    }

    /**
     * 표준 인증 이미지 삭제
     */
    @PostMapping("/v1/admin/verification-images/{imageId}/delete")
    fun deleteStandardImage(
        @PathVariable imageId: Long,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            adminService.deleteStandardVerificationImage(imageId)
            redirectAttributes.addFlashAttribute("success", "표준 인증 이미지가 성공적으로 삭제되었습니다.")
        } catch (e: Exception) {
            redirectAttributes.addFlashAttribute("error", "이미지 삭제에 실패했습니다: ${e.message}")
        }
        return "redirect:/v1/admin/verification-images"
    }
}

