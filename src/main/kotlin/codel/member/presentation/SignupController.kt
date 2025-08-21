package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.presentation.request.PhoneVerificationRequest
import codel.member.presentation.response.SignUpStatusResponse
import codel.member.presentation.swagger.SignupControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/signup")
class SignupController(
    private val memberService: MemberService
) : SignupControllerSwagger {

    @GetMapping("/status")
    override fun getSignupStatus(
        @LoginMember member: Member
    ): ResponseEntity<SignUpStatusResponse> {
        val currentMember = memberService.findMember(member.getIdOrThrow())
        return ResponseEntity.ok(SignUpStatusResponse.from(currentMember))
    }

    @PostMapping("/phone/verify")
    override fun completePhoneVerification(
        @LoginMember member: Member,
    ): ResponseEntity<Unit> {
        memberService.completePhoneVerification(member)

        return ResponseEntity.ok().build()
    }
}
