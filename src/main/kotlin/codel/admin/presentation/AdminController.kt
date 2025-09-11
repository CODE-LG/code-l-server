package codel.admin.presentation

import codel.admin.business.AdminService
import codel.admin.domain.Admin
import codel.admin.exception.AdminException
import codel.admin.presentation.request.AdminLoginRequest
import codel.member.domain.Member
import codel.question.domain.QuestionCategory
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
class AdminController(
    private val adminService: AdminService,
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
        val member = adminService.findMember(memberId)
        val codeImages = member.getProfileOrThrow().getCodeImageOrThrow()
        val faceImages = member.getProfileOrThrow().getFaceImageOrThrow()

        model.addAttribute("member", member)
        model.addAttribute("codeImages", codeImages)
        model.addAttribute("faceImages", faceImages)

        return "memberDetail"
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

    @GetMapping("/v1/admin/members")
    fun memberList(
        model: Model,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) status: String?,
        @PageableDefault(size = 20) pageable: Pageable
    ): String {
        val members: Page<Member> = adminService.findMembersWithFilter(keyword, status, pageable)
        model.addAttribute("members", members)
        model.addAttribute("param", mapOf("keyword" to (keyword ?: ""), "status" to (status ?: "")))
        return "memberList"
    }

    // ========== 질문 관리 ==========
    
    @GetMapping("/v1/admin/questions")
    fun questionList(
        model: Model,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false) isActive: Boolean?,
        @PageableDefault(size = 20) pageable: Pageable
    ): String {
        val questions = adminService.findQuestionsWithFilter(keyword, category, isActive, pageable)
        model.addAttribute("questions", questions)
        model.addAttribute("categories", QuestionCategory.values())
        model.addAttribute("param", mapOf(
            "keyword" to (keyword ?: ""),
            "category" to (category ?: ""),
            "isActive" to (isActive?.toString() ?: "")
        ))
        return "questionList"
    }

    @GetMapping("/v1/admin/questions/new")
    fun questionForm(model: Model): String {
        model.addAttribute("categories", QuestionCategory.values())
        return "questionForm"
    }

    @PostMapping("/v1/admin/questions")
    fun createQuestion(
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(defaultValue = "true") isActive: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val questionCategory = QuestionCategory.valueOf(category)
            adminService.createQuestion(content, questionCategory, description, isActive)
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
        return "questionEditForm"
    }

    @PostMapping("/v1/admin/questions/{questionId}")
    fun updateQuestion(
        @PathVariable questionId: Long,
        @RequestParam content: String,
        @RequestParam category: String,
        @RequestParam(required = false) description: String?,
        @RequestParam(defaultValue = "false") isActive: Boolean,
        redirectAttributes: RedirectAttributes
    ): String {
        try {
            val questionCategory = QuestionCategory.valueOf(category)
            adminService.updateQuestion(questionId, content, questionCategory, description, isActive)
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
}
