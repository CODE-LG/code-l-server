package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.PhoneVerificationRequest
import codel.member.presentation.response.SignUpStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "회원가입", description = "단계별 회원가입 관련 API")
interface SignupControllerSwagger {

    @Operation(
        summary = "회원가입 진행 상태 조회",
        description = "현재 회원의 회원가입 진행 상태와 다음 단계를 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(responseCode = "401", description = "인증 실패"),
            ApiResponse(responseCode = "404", description = "회원 정보 없음")
        ]
    )
    fun getSignupStatus(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<SignUpStatusResponse>

    @Operation(
        summary = "전화번호 인증 완료",
        description = "전화번호 인증을 완료하고 다음 단계(Essential Profile)로 진행할 수 있도록 상태를 변경합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "인증 완료"),
            ApiResponse(responseCode = "400", description = "잘못된 인증 정보 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun completePhoneVerification(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<Unit>
}
