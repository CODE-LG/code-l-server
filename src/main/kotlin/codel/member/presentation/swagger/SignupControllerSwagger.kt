package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.HiddenProfileRequest
import codel.member.presentation.request.PersonalityProfileRequest
import codel.member.presentation.response.SignUpStatusResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.multipart.MultipartFile

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
        @Parameter(hidden = true) @LoginMember member: Member,
    ): ResponseEntity<Unit>

    @Operation(
        summary = "Open Profile 정보 등록",
        description = "기본 프로필 정보를 등록합니다. (이미지 제외)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 입력 데이터 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerEssentialProfile(
        @Parameter(hidden = true) @LoginMember member: Member,
        @RequestBody request: EssentialProfileRequest
    ): ResponseEntity<Unit>

    @Operation(
        summary = "Open Profile 이미지 등록",
        description = "기본 프로필 이미지를 등록하고 Open Profile을 완료합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 완료"),
            ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerEssentialImages(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(description = "코드 이미지 파일들 (1-3장)") images: List<MultipartFile>
    ): ResponseEntity<Unit>

    @Operation(
        summary = "Personality Profile 등록",
        description = "성격/취향 프로필 정보를 등록하고 Personality Profile을 완료합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 완료"),
            ApiResponse(responseCode = "400", description = "잘못된 입력 데이터 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerPersonalityProfile(
        @Parameter(hidden = true) @LoginMember member: Member,
        @RequestBody request: PersonalityProfileRequest
    ): ResponseEntity<Unit>

    @Operation(
        summary = "Hidden Profile 정보 등록",
        description = "히든 프로필 정보를 등록합니다. (이미지 제외)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 입력 데이터 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerHiddenProfile(
        @Parameter(hidden = true) @LoginMember member: Member,
        @RequestBody request: HiddenProfileRequest
    ): ResponseEntity<Unit>

    @Operation(
        summary = "Hidden Profile 이미지 등록",
        description = "히든 프로필 이미지를 등록하고 Hidden Profile을 완료합니다. 회원가입이 완료되어 PENDING 상태로 변경됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 완료 (PENDING 상태로 변경)"),
            ApiResponse(responseCode = "400", description = "잘못된 이미지 파일 또는 단계 오류"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerHiddenImages(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(description = "얼굴 이미지 파일들 (3장)") images: List<MultipartFile>
    ): ResponseEntity<Unit>
}
