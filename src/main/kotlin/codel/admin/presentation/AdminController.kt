package codel.admin.presentation

import codel.admin.business.AdminService
import codel.admin.domain.Admin
import codel.admin.presentation.request.AdminLoginRequest
import codel.auth.business.AuthService
import codel.member.presentation.request.MemberLoginRequest
import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@Controller
class AdminController(
    private val adminService: AdminService,
    private val authService: AuthService,
) {
    @GetMapping("/v1/admin/login")
    fun login(): String = "login"

    @PostMapping("/v1/admin/login")
    fun login(
        @RequestBody adminLoginRequest: AdminLoginRequest,
        response: HttpServletResponse,
    ): String {
        val admin = Admin(adminLoginRequest.password)
        adminService.loginAdmin(admin)

        val token =
            authService.provideToken(
                MemberLoginRequest(
                    oauthType = admin.oauthType,
                    oauthId = admin.oauthId,
                ),
            )

        addCookie(token, response)

        return "redirect:/v1/admin/home"
    }

    private fun addCookie(
        token: String,
        response: HttpServletResponse,
    ) {
        val cookie = Cookie("access_token", token)
        cookie.path = "/v1/admin"
        cookie.isHttpOnly = true
        cookie.maxAge = 86400

        response.addCookie(cookie)
    }

    @GetMapping("/v1/admin/home")
    fun home(model: Model): String {
        val members = adminService.findPendingMembers()
        model.addAttribute("members", members)

        return "home"
    }

    @PostMapping("/v1/admin/approval/{targetId}")
    fun approveMember(
        @PathVariable targetId: Long,
    ): String {
        adminService.approveMemberProfile(targetId)

        return "redirect:/v1/admin/home"
    }

    @PostMapping("/v1/admin/reject/{targetId}")
    fun rejectMember(
        @PathVariable targetId: Long,
        @RequestBody rejectReason: String,
    ): String {
        adminService.rejectMemberProfile(targetId, rejectReason)

        return "redirect:/v1/admin/home"
    }
}
