package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.EssentialProfileRequest
import codel.member.presentation.request.HiddenProfileRequest
import codel.member.presentation.request.PersonalityProfileRequest
import codel.member.presentation.response.SignUpStatusResponse
import codel.verification.presentation.response.VerificationImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
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
        description = """
            히든 프로필 이미지를 등록합니다. 앱 버전과 회원 상태에 따라 다르게 동작합니다.

            **정상 가입 (PERSONALITY_COMPLETED):**
            - 히든 프로필 이미지를 등록하고 다음 단계로 진행합니다.

            **재심사 (REJECT):**
            - 구버전 앱(1.2.0 미만): 히든 이미지를 등록하고 PENDING 상태로 변경 (하위호환)
            - 신규 앱(1.2.0 이상): 새로운 재심사 API(/v1/profile/review/resubmit)를 사용하도록 안내

            **X-App-Version 헤더:**
            - 앱 버전을 명시하지 않으면 구버전으로 간주되어 하위호환 로직이 적용됩니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "등록 완료"),
            ApiResponse(responseCode = "400", description = "잘못된 이미지 파일, 단계 오류, 또는 신규 앱에서 재심사 시도"),
            ApiResponse(responseCode = "401", description = "인증 실패")
        ]
    )
    fun registerHiddenImages(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(description = "얼굴 이미지 파일들 (3장)") images: List<MultipartFile>,
        @Parameter(description = "앱 버전 (예: 1.2.0)") appVersion: String?
    ): ResponseEntity<Any>

    @Operation(
        summary = "사용자 인증 이미지 제출",
        description = """
            표준 이미지를 참고하여 촬영한 본인 인증 이미지를 제출합니다.

            **요구사항:**
            - 회원 상태가 HIDDEN_COMPLETED 또는 REJECT여야 함
            - multipart/form-data로 전송
            - 이미지 파일 크기: 최대 10MB
            - 허용된 확장자: jpg, jpeg, png, gif, webp

            **제출 과정:**
            1. 표준 이미지 조회 (GET /v1/verification/standard-image)
            2. 표준 이미지를 보고 동일한 자세로 촬영
            3. 촬영한 이미지를 본 API로 제출
            4. 회원 상태가 PENDING (심사 대기)으로 변경

            **재제출:**
            - 재제출 가능 (기존 이미지는 유지, 이력 관리)
            - 거절 후 재제출 시에도 동일한 API 사용

            ※ Authorization 헤더에 JWT 토큰을 포함해야 합니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "인증 이미지 제출 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = VerificationImageResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 - 회원 상태가 올바르지 않거나 파일 검증 실패",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자 - JWT 토큰이 없거나 유효하지 않음",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "404",
                description = "표준 인증 이미지를 찾을 수 없음",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류 - S3 업로드 실패 등",
                content = [Content()]
            )
        ]
    )
    fun submitVerificationImage(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(description = "참조한 표준 이미지 ID") standardImageId: Long,
        @Parameter(description = "사용자가 촬영한 인증 이미지 파일") userImage: MultipartFile
    ): ResponseEntity<VerificationImageResponse>
}
