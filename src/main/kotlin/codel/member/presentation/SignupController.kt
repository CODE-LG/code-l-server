package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.business.SignupService
import codel.member.domain.Member
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.PhoneVerificationRequest
import codel.member.presentation.response.SignUpStatusResponse
import codel.member.presentation.swagger.SignupControllerSwagger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/signup")
class SignupController(
    private val memberService: MemberService,
    private val signupService: SignupService
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

    @PostMapping("/essential/profile")
    override fun registerEssentialProfile(
        @LoginMember member: Member,
        @RequestBody request: EssentialProfileRequest
    ): ResponseEntity<Unit> {
        signupService.registerEssentialProfile(member, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/essential/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun registerEssentialImages(
        @LoginMember member: Member,
        @RequestPart images: List<MultipartFile>
    ): ResponseEntity<Unit> {
        signupService.registerEssentialImages(member, images)
        return ResponseEntity.ok().build()
    }
}
