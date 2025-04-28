package codel.member.presentation

import codel.auth.business.AuthService
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.ProfileSavedRequest
import codel.member.presentation.response.MemberLoginResponse
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.swagger.MemberControllerSwagger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class MemberController(
    private val memberService: MemberService,
    private val authService: AuthService,
) : MemberControllerSwagger {
    @PostMapping("/v1/member/login")
    override fun loginMember(
        @RequestBody request: MemberLoginRequest,
    ): ResponseEntity<MemberLoginResponse> {
        val member = memberService.loginMember(request.toMember())
        val token = authService.provideToken(member)
        return ResponseEntity
            .ok()
            .header("Authorization", "Bearer $token")
            .body(MemberLoginResponse(member.memberStatus))
    }

    @PostMapping("/v1/member/profile")
    override fun saveProfile(
        @LoginMember member: Member,
        @RequestBody request: ProfileSavedRequest,
    ): ResponseEntity<Unit> {
        memberService.saveProfile(member, request.toProfile())
        return ResponseEntity.ok().build()
    }

    @PostMapping(
        "/v1/member/codeimage",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    override fun saveCodeImage(
        @LoginMember member: Member,
        @RequestPart files: List<MultipartFile>,
    ): ResponseEntity<Unit> {
        memberService.saveCodeImage(member, files)
        return ResponseEntity.ok().build()
    }

    @PostMapping(
        "/v1/member/faceimage",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE],
    )
    override fun saveFaceImage(
        @LoginMember member: Member,
        @RequestPart files: List<MultipartFile>,
    ): ResponseEntity<Unit> {
        memberService.saveFaceImage(member, files)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/v1/member/fcmtoken")
    override fun saveFcmToken(
        @LoginMember member: Member,
        @RequestBody fcmToken: String,
    ): ResponseEntity<Unit> {
        memberService.saveFcmToken(member, fcmToken)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/v1/member/profile")
    override fun findMemberProfile(
        @LoginMember member: Member,
    ): ResponseEntity<MemberProfileResponse> {
        val findMember = memberService.findMemberProfile(member)
        return ResponseEntity.ok(MemberProfileResponse.toResponse(findMember))
    }
}
