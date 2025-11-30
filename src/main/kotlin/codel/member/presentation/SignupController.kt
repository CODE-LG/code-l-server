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
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.verification.presentation.response.VerificationImageResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/signup")
class SignupController(
    private val memberService: MemberService,
    private val signupService: SignupService,
    private val asyncNotificationService: IAsyncNotificationService
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
        return ResponseEntity.ok().build()
    }

    @PostMapping("/verification/image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun submitVerificationImage(
        @LoginMember member: Member,
        @RequestParam standardImageId: Long,
        @RequestPart userImage: MultipartFile
    ): ResponseEntity<VerificationImageResponse> {
        val response = signupService.submitVerificationImage(member, standardImageId, userImage)
        // 비동기로 알림 전송
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "${member.getProfileOrThrow().getCodeNameOrThrow()}님이 심사를 요청하였습니다.",
                    body = "code:L 프로필 심사 요청이 왔습니다.",
                ),
        )
        return ResponseEntity.ok(response)
    }
}
