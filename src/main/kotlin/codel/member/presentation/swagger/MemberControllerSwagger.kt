package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.ProfileSavedRequest
import codel.member.presentation.response.MemberLoginResponse
import codel.member.presentation.response.MemberProfileResponse
import codel.member.presentation.response.MemberRecommendResponse
import codel.member.presentation.response.MemberRecommendResponses
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

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

    @Operation(summary = "이미지를 제외한 프로필 받기", description = "이미지를 제외한 프로필을 입력받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "프로필 성공적으로 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun saveProfile(
        @LoginMember member: Member,
        @RequestBody request: ProfileSavedRequest,
    ): ResponseEntity<Unit>

    @Operation(summary = "코드 프로필 이미지 받기", description = "코드 프로필 이미지 받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "코드 프로필 이미지 성공적으로 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun saveCodeImage(
        @LoginMember member: Member,
        @RequestPart files: List<MultipartFile>,
    ): ResponseEntity<Unit>

    @Operation(summary = "페이지 이미지 받기", description = "페이즈 이미지를 3장 받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "페이스 이미지 성공적으로 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun saveFaceImage(
        @LoginMember member: Member,
        @RequestPart files: List<MultipartFile>,
    ): ResponseEntity<Unit>

    @Operation(summary = "사용자별 fcm 토큰 받기", description = "사용자의 디바이스 별 fcm 토큰을 저장합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "fcm 토큰 저장됨"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun saveFcmToken(
        @LoginMember member: Member,
        @RequestBody fcmToken: String,
    ): ResponseEntity<Unit>

    @Operation(summary = "작성된 사용자의 코드 프로필 확인", description = "작성된 사용자의 코드 프로필 정보를 받을 수 있습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "사용자 프로필을 성공적으로 가져옴"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun findMemberProfile(
        @LoginMember member: Member,
    ): ResponseEntity<MemberProfileResponse>

    @Operation(summary = "홈 코드 추천 매칭 조회", description = "코드 추천 매칭 목록을 받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "코드 추천 매칭 조회 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun recommendMembers(
        @LoginMember member: Member,
    ): ResponseEntity<MemberRecommendResponses>

    @Operation(summary = "홈 파도타기 조회", description = "홈 파도 타기 목록을 받습니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "홈 파도 타기 목록 조회 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getDailyRecommendMembers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
    ): ResponseEntity<Page<MemberRecommendResponse>>
}
