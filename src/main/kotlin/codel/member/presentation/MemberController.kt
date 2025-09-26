package codel.member.presentation

import codel.auth.business.AuthService
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.response.*
import codel.member.presentation.swagger.MemberControllerSwagger
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
            .body(MemberLoginResponse(member.getIdOrThrow(), member.memberStatus))
    }

    @PostMapping("/v1/member/fcmtoken")
    override fun saveFcmToken(
        @LoginMember member: Member,
        @RequestBody fcmToken: String,
    ): ResponseEntity<Unit> {
        memberService.saveFcmToken(member, fcmToken)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/v1/member/me")
    override fun findMyProfile(
        @LoginMember member: Member,
    ): ResponseEntity<FullProfileResponse> {
        val findMyProfile = memberService.findMyProfile(member)
        return ResponseEntity.ok(findMyProfile)
    }

    @GetMapping("/v1/member/recommend")
    @Deprecated("Use /api/v1/recommendations/daily-code-matching or /api/v1/recommendations/legacy/recommend instead")
    override fun recommendMembers(
        @LoginMember member: Member,
    ): ResponseEntity<MemberRecommendResponse> {
        val members = memberService.recommendMembers(member)
        return ResponseEntity.ok(MemberRecommendResponse.from(members))
    }

    @GetMapping("/v1/member/all")
    @Deprecated("Use /api/v1/recommendations/random or /api/v1/recommendations/legacy/all instead")
    override fun getRecommendMemberAtTenHourCycle(
        @LoginMember member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "8") size: Int,
    ): ResponseEntity<Page<FullProfileResponse>> {
        val memberPage = memberService.getRandomMembers(member, page, size)

        return ResponseEntity.ok(
            memberPage.map { member ->
                FullProfileResponse.createOpen(member)
            },
        )
    }

    @GetMapping("/v1/members/{id}")
    override fun getMemberProfileDetail(
        @LoginMember me: Member,
        @PathVariable id: Long,
    ): ResponseEntity<MemberProfileDetailResponse> {
        val memberProfileDetail = memberService.findMemberProfile(me, id)

        return ResponseEntity.ok(memberProfileDetail)
    }

    @DeleteMapping("/v1/member/me")
    override fun withdrawMember(
        @LoginMember member: Member,
    ): ResponseEntity<Void> {
        memberService.withdrawMember(member)
        return ResponseEntity.noContent().build()
    }
}
