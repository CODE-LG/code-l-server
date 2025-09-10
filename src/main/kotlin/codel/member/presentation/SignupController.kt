package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.business.SignupService
import codel.member.domain.Member
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.HiddenProfileRequest
import codel.member.presentation.request.PersonalityProfileRequest
import codel.member.presentation.response.SignUpStatusResponse
import codel.member.presentation.swagger.SignupControllerSwagger
import codel.notification.business.NotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/signup")
class SignupController(
    private val memberService: MemberService,
    private val signupService: SignupService,
    private val notificationService: NotificationService
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
        signupService.completePhoneVerification(member)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/open/profile")
    override fun registerEssentialProfile(
        @LoginMember member: Member,
        @RequestBody request: EssentialProfileRequest
    ): ResponseEntity<Unit> {
        signupService.registerEssentialProfile(member, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/open/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun registerEssentialImages(
        @LoginMember member: Member,
        @RequestPart images: List<MultipartFile>
    ): ResponseEntity<Unit> {
        signupService.registerEssentialImages(member, images)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/open/personality")
    override fun registerPersonalityProfile(
        @LoginMember member: Member,
        @RequestBody request: PersonalityProfileRequest
    ): ResponseEntity<Unit> {
        signupService.registerPersonalityProfile(member, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hidden/profile")
    override fun registerHiddenProfile(
        @LoginMember member: Member,
        @RequestBody request: HiddenProfileRequest
    ): ResponseEntity<Unit> {
        signupService.registerHiddenProfile(member, request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/hidden/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun registerHiddenImages(
        @LoginMember member: Member,
        @RequestPart images: List<MultipartFile>
    ): ResponseEntity<Unit> {
        signupService.registerHiddenImages(member, images)
        notificationService.send(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "심사가 완료되었습니다.",
                    body = "code:L 프로필 심사가 완료되었습니다.",
                ),
        )
        return ResponseEntity.ok().build()
    }
}
