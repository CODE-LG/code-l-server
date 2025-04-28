package codel.admin.presentation

import codel.admin.business.AdminService
import codel.admin.domain.Admin
import codel.admin.exception.AdminException
import codel.admin.presentation.request.AdminLoginRequest
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

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
        val members = adminService.findPendingMembers()
        model.addAttribute("members", members)

        return "home"
    }

    @GetMapping("/v1/admin/member/{memberId}")
    fun findMemberDetail(
        model: Model,
        @PathVariable memberId: Long,
    ): String {
        val member = adminService.findMember(memberId)
        model.addAttribute("member", member)

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
}
