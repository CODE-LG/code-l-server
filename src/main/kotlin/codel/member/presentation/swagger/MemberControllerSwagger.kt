package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.response.*
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Member", description = "회원 관련 API")
interface MemberControllerSwagger {
    @Operation(summary = "로그인 및 회원 저장 후 분기 반환", description = "소셜 로그인 정보를 기반으로 회원을 저장하고, JWT 토큰과 회원 분기를 반환합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 로그인 및 회원 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun loginMember(
        @RequestBody request: MemberLoginRequest,
    ): ResponseEntity<MemberLoginResponse>

    @Operation(
        summary = "사용자별 fcm 토큰 받기",
        description = "사용자의 디바이스 별 fcm 토큰을 저장합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "fcm 토큰 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun saveFcmToken(
        @Parameter(hidden = true) @LoginMember member: Member,
        @RequestBody fcmToken: String,
    ): ResponseEntity<Unit>

    @Operation(
        summary = "내 프로필 조회",
        description = "작성된 사용자의 전체 프로필 정보를 받을 수 있습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "사용자 프로필을 성공적으로 가져옴"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun findMyProfile(
        @Parameter(hidden = true) @LoginMember member: Member,
    ): ResponseEntity<FullProfileResponse>

    @Deprecated("Use /api/v1/recommendations/daily-code-matching instead")
    @Operation(
        summary = "[Deprecated] 홈 코드 추천 매칭 조회", 
        description = "⚠️ DEPRECATED: /api/v1/recommendations/daily-code-matching을 사용하세요. " +
                     "코드 추천 매칭 목록을 받습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "코드 추천 매칭 조회 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun recommendMembers(
        @Parameter(hidden = true) @LoginMember member: Member,
    ): ResponseEntity<MemberRecommendResponse>

    @Deprecated("Use /api/v1/recommendations/random instead")
    @Operation(
        summary = "[Deprecated] 홈 파도타기 조회",
        description = "⚠️ DEPRECATED: /api/v1/recommendations/random을 사용하세요. " +
                     "홈 파도 타기 목록을 받습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "홈 파도 타기 목록 조회 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getRecommendMemberAtTenHourCycle(
        @Parameter(hidden = true) @LoginMember member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "8") size: Int,
    ): ResponseEntity<Page<FullProfileResponse>>

    @Operation(
        summary = "회원 상세 조회",
        description = "회원 정보를 상세 조회합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "회원 상세 조회 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getMemberProfileDetail(
        @Parameter(hidden = true) @LoginMember me: Member,
        @PathVariable id: Long,
    ): ResponseEntity<MemberProfileDetailResponse>

    @Operation(
        summary = "회원 탈퇴",
        description = "현재 로그인한 회원의 계정을 탈퇴 처리합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun withdrawMember(
        @Parameter(hidden = true) @LoginMember member: Member,
    ): ResponseEntity<Void>
}
