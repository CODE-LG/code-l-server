package codel.recommendation.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.recommendation.presentation.response.*
import codel.member.presentation.response.MemberRecommendResponse
import codel.member.presentation.response.FullProfileResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 추천 시스템 API Swagger 문서 인터페이스
 */
@Tag(name = "Recommendation", description = "추천 시스템 API - 지역 기반 버킷 정책과 시간대별 추천을 제공합니다")
interface RecommendationSwagger {
    
    @Operation(summary = "통합 추천 조회", description = "사용자의 현재 상황에 맞는 최적의 추천을 제공합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "통합 추천 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
        ]
    )
    fun getRecommendation(
        @LoginMember member: Member,
        @Parameter(description = "코드타임을 우선적으로 원하는지 여부")
        @RequestParam(defaultValue = "false") preferCodeTime: Boolean
    ): ResponseEntity<RecommendationResponse>
    
    @Operation(summary = "오늘의 코드매칭 조회", description = "24시간 유지되는 오늘의 코드매칭을 조회합니다.")
    fun getDailyCodeMatching(
        @LoginMember member: Member
    ): ResponseEntity<MemberRecommendResponse>
    
    @Operation(summary = "코드타임 조회", description = "현재 활성 시간대의 코드타임을 조회합니다.")
    fun getCodeTime(
        @LoginMember member: Member
    ): ResponseEntity<CodeTimeResponse>
    
    @Operation(summary = "특정 시간대 코드타임 조회", description = "특정 시간대의 코드타임을 조회합니다.")
    fun getCodeTimeBySlot(
        @LoginMember member: Member,
        @Parameter(description = "조회할 시간대") @PathVariable timeSlot: String
    ): ResponseEntity<CodeTimeResponse>
    
    @Operation(summary = "추천 현황 종합 조회", description = "사용자의 추천 관련 모든 정보를 종합적으로 조회합니다.")
    fun getRecommendationOverview(
        @LoginMember member: Member
    ): ResponseEntity<RecommendationOverviewResponse>
    
    @Operation(summary = "추천 시스템 설정 조회", description = "현재 추천 시스템의 모든 설정값을 조회합니다.")
    fun getRecommendationSettings(): ResponseEntity<Map<String, Any>>
    
    @Operation(summary = "추천 강제 새로고침", description = "특정 사용자의 추천을 강제로 새로 생성합니다.")
    fun forceRefreshRecommendation(
        @LoginMember member: Member,
        @Parameter(description = "추천 타입") @RequestParam type: String,
        @Parameter(description = "코드타임 시간대") @RequestParam(required = false) timeSlot: String?
    ): ResponseEntity<Map<String, Any>>
    
    @Operation(summary = "시스템 헬스체크", description = "추천 시스템의 전반적인 상태를 확인합니다.")
    fun getSystemHealthCheck(): ResponseEntity<Map<String, Any>>
    
    // ===== Legacy API 호환성 (Deprecated) =====
    
    @Deprecated("Use getDailyCodeMatching instead")
    @Operation(
        summary = "[Deprecated] 홈 코드 추천 매칭 조회", 
        description = "⚠️ DEPRECATED: /api/v1/recommendations/daily-code-matching을 사용하세요."
    )
    fun legacyRecommendMembers(
        @LoginMember member: Member
    ): ResponseEntity<MemberRecommendResponse>
    
    @Deprecated("Use getRandomMembers instead") 
    @Operation(
        summary = "[Deprecated] 홈 파도타기 조회",
        description = "⚠️ DEPRECATED: /api/v1/recommendations/random을 사용하세요."
    )
    fun legacyGetRandomMembers(
        @LoginMember member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "8") size: Int
    ): ResponseEntity<Page<FullProfileResponse>>
    
    // ===== 새로운 통합 추천 API =====
    
    @Operation(
        summary = "랜덤 회원 추천 (파도타기)", 
        description = "페이지네이션을 지원하는 랜덤 회원 추천을 제공합니다."
    )
    fun getRandomMembers(
        @LoginMember member: Member,
        @Parameter(description = "페이지 번호 (0부터 시작)") @RequestParam(defaultValue = "0") page: Int,
        @Parameter(description = "페이지 크기") @RequestParam(defaultValue = "8") size: Int
    ): ResponseEntity<Page<FullProfileResponse>>
}
