package codel.member.presentation

import codel.auth.business.AuthService
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.WithdrawnRequest
import codel.member.presentation.request.UpdateRepresentativeQuestionRequest
import codel.member.presentation.response.*
import codel.member.presentation.swagger.MemberControllerSwagger
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
        @RequestBody request : WithdrawnRequest
    ): ResponseEntity<Void> {
        memberService.withdrawMember(member, request.reason)
        return ResponseEntity.noContent().build()
    }

    // ========== 프로필 수정 ==========

    /**
     * 코드 이미지 수정
     * - Multipart 파일로 1~3개의 이미지를 받아 전체 교체
     * - 상태가 PENDING으로 변경되어 재심사 진행
     */
    @PutMapping("/v1/member/me/profile/code-images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun updateCodeImages(
        @LoginMember member: Member,
        @RequestPart("codeImages") codeImages: List<MultipartFile>,
    ): ResponseEntity<UpdateCodeImagesResponse> {
        val response = memberService.updateCodeImages(member, codeImages)
        return ResponseEntity.ok(response)
    }

    /**
     * 대표 질문 및 답변 수정
     * - 기존 질문 ID와 새로운 답변으로 수정
     */
    @PutMapping("/v1/member/me/profile/representative-question")
    override fun updateRepresentativeQuestion(
        @LoginMember member: Member,
        @RequestBody request: UpdateRepresentativeQuestionRequest
    ): ResponseEntity<UpdateRepresentativeQuestionResponse> {
        val response = memberService.updateRepresentativeQuestion(
            member,
            request.representativeQuestionId,
            request.representativeAnswer
        )
        return ResponseEntity.ok(response)
    }
}
