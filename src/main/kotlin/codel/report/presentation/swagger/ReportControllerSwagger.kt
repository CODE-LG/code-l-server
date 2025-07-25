package codel.report.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.ProfileSavedRequest
import codel.member.presentation.response.MemberLoginResponse
import codel.member.presentation.response.MemberProfileDetailResponse
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.response.MemberRecommendResponses
import codel.member.presentation.response.MemberResponse
import codel.report.presentation.request.ReportRequest
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Tag(name = "Report", description = "신고 관련 API")
interface ReportControllerSwagger {
    @Operation(
        summary = "회원 신고 조회",
        description = "다른 회원을 신고할 수 있습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "회원 신고 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun reportMember(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestBody reportRequest: ReportRequest,
    ): ResponseEntity<Unit>
}
