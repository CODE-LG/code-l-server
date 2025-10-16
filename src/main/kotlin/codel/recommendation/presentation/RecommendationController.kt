package codel.recommendation.presentation

import codel.config.argumentresolver.LoginMember
import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.business.RecommendationService
import codel.recommendation.domain.RecommendationType
import codel.recommendation.presentation.response.*
import codel.recommendation.presentation.swagger.RecommendationSwagger
import codel.member.presentation.response.MemberRecommendResponse
import codel.member.presentation.response.FullProfileResponse
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 추천 시스템 API Controller
 *
 * 새로운 지역 기반 버킷 정책 + 시간대별 추천 시스템 제공
 * 기존 랜덤 추천을 대체하는 고도화된 추천 서비스
 */
@RestController
@RequestMapping("/v1/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) : RecommendationSwagger, Loggable {

    @GetMapping("/daily-code-matching")
    override fun getDailyCodeMatching(
        @LoginMember member: Member
    ): ResponseEntity<MemberRecommendResponse> {
        log.info { "오늘의 코드매칭 API 호출 - userId: ${member.getIdOrThrow()}" }

        val members = recommendationService.getDailyCodeMatching(member)

        log.info { "오늘의 코드매칭 API 응답 - userId: ${member.getIdOrThrow()}, count: ${members.size}" }

        return ResponseEntity.ok(MemberRecommendResponse.from(members))
    }

    @GetMapping("/random")
    override fun getCodeTime(
        @LoginMember member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "8") size: Int
    ): ResponseEntity<Page<FullProfileResponse>> {
        log.info { "코드타임 API 호출 - userId: ${member.getIdOrThrow()}" }

        // Service에서 DTO 변환까지 완료된 결과 받기
        val responsePage = recommendationService.getCodeTime(member, page, size)

        return ResponseEntity.ok(responsePage)
    }

//    @GetMapping("/random")
//    override fun getRandomMembers(
//        @LoginMember member: Member,
//        @RequestParam(defaultValue = "0") page: Int,
//        @RequestParam(defaultValue = "8") size: Int
//    ): ResponseEntity<Page<FullProfileResponse>> {
//        log.info {
//            "랜덤 추천 (파도타기) API 호출 - userId: ${member.getIdOrThrow()}, " +
//                    "page: $page, size: $size"
//        }
//
//        val memberPage = recommendationService.getRandomMembers(member, page, size)
//
//        log.info {
//            "랜덤 추천 (파도타기) API 응답 - userId: ${member.getIdOrThrow()}, " +
//                    "totalElements: ${memberPage.totalElements}, currentPage: ${memberPage.number}"
//        }
//
//        return ResponseEntity.ok(
//            memberPage.map { memberEntity ->
//                FullProfileResponse.createOpen(memberEntity)
//            }
//        )
//    }

//    @GetMapping("/code-time/{timeSlot}")
//    fun getCodeTimeBySlot(
//        @LoginMember member: Member,
//        @PathVariable timeSlot: String
//    ): ResponseEntity<CodeTimeResponse> {
//        log.info { "특정 시간대 코드타임 API 호출 - userId: ${member.getIdOrThrow()}, timeSlot: $timeSlot" }
//
//        val result = recommendationService.getCodeTimeBySlot(member, timeSlot)
//        val response = CodeTimeResponse.from(result)
//
//        log.info {
//            "특정 시간대 코드타임 API 응답 - userId: ${member.getIdOrThrow()}, " +
//            "timeSlot: $timeSlot, count: ${result.recommendationCount}"
//        }
//
//        return ResponseEntity.ok(response)
//    }
//
//    @GetMapping("/overview")
//    fun getRecommendationOverview(
//        @LoginMember member: Member
//    ): ResponseEntity<RecommendationOverviewResponse> {
//        log.info { "추천 현황 종합 조회 API 호출 - userId: ${member.getIdOrThrow()}" }
//
//        val overview = recommendationService.getRecommendationOverview(member)
//        val response = RecommendationOverviewResponse.from(overview)
//
//        log.info {
//            "추천 현황 종합 조회 API 응답 - userId: ${member.getIdOrThrow()}, " +
//            "hasDaily: ${overview.hasDailyCodeMatching}, " +
//            "isCodeTimeActive: ${overview.isCodeTimeActive}"
//        }
//
//        return ResponseEntity.ok(response)
//    }
//
    @GetMapping("/settings")
    fun getRecommendationSettings(): ResponseEntity<Map<String, Any>> {
        log.info { "추천 시스템 설정 조회 API 호출" }

        val settings = recommendationService.getRecommendationSettings()

        log.info { "추천 시스템 설정 조회 API 응답 - settings: $settings" }

        return ResponseEntity.ok(settings)
    }
//
//    @PostMapping("/refresh")
//    fun forceRefreshRecommendation(
//        @LoginMember member: Member,
//        @RequestParam type: String,
//        @RequestParam(required = false) timeSlot: String?
//    ): ResponseEntity<Map<String, Any>> {
//        log.info {
//            "추천 강제 새로고침 API 호출 - userId: ${member.getIdOrThrow()}, " +
//            "type: $type, timeSlot: $timeSlot"
//        }
//
//        // String을 RecommendationType으로 변환
//        val recommendationType = try {
//            RecommendationType.valueOf(type)
//        } catch (e: IllegalArgumentException) {
//            log.warn { "유효하지 않은 추천 타입 - userId: ${member.getIdOrThrow()}, type: $type" }
//            val errorResponse: Map<String, Any> = mapOf(
//                "success" to false,
//                "error" to "유효하지 않은 추천 타입입니다. DAILY_CODE_MATCHING 또는 CODE_TIME을 사용하세요.",
//                "validTypes" to RecommendationType.values().map { it.name }
//            )
//            return ResponseEntity.badRequest().body(errorResponse)
//        }
//
//        return try {
//            val result = recommendationService.forceRefreshRecommendation(member, recommendationType, timeSlot)
//
//            val response: Map<String, Any> = mapOf(
//                "success" to true,
//                "type" to type,
//                "timeSlot" to (timeSlot ?: ""),
//                "result" to result,
//                "refreshedAt" to System.currentTimeMillis()
//            )
//
//            log.info {
//                "추천 강제 새로고침 API 응답 - userId: ${member.getIdOrThrow()}, " +
//                "type: $type, success: true"
//            }
//
//            ResponseEntity.ok(response)
//
//        } catch (e: IllegalStateException) {
//            log.warn {
//                "추천 강제 새로고침 실패 - userId: ${member.getIdOrThrow()}, " +
//                "type: $type, error: ${e.message}"
//            }
//
//            val errorResponse: Map<String, Any> = mapOf(
//                "success" to false,
//                "error" to (e.message ?: "강제 새로고침에 실패했습니다."),
//                "type" to type,
//                "timeSlot" to (timeSlot ?: "")
//            )
//
//            ResponseEntity.badRequest().body(errorResponse)
//        }
//    }
//
//    @GetMapping("/health")
//    fun getSystemHealthCheck(): ResponseEntity<Map<String, Any>> {
//        log.info { "시스템 헬스체크 API 호출" }
//
//        val healthCheck = recommendationService.getSystemHealthCheck()
//
//        log.info { "시스템 헬스체크 API 응답 - status: ${healthCheck["systemStatus"]}" }
//
//        return ResponseEntity.ok(healthCheck)
//    }
//
//    /**
//     * 제외 로직 통계 조회 (디버깅용)
//     * 어떤 사용자들이 왜 제외되었는지 확인할 수 있습니다.
//     */
//    @GetMapping("/exclusion-stats")
//    fun getExclusionStatistics(
//        @LoginMember member: Member,
//        @RequestParam(defaultValue = "DAILY_CODE_MATCHING") type: String
//    ): ResponseEntity<Map<String, Any>> {
//        log.info {
//            "제외 통계 조회 API 호출 - userId: ${member.getIdOrThrow()}, type: $type"
//        }
//
//        val recommendationType = try {
//            RecommendationType.valueOf(type)
//        } catch (e: IllegalArgumentException) {
//            val errorResponse: Map<String, Any> = mapOf(
//                "error" to "유효하지 않은 추천 타입입니다.",
//                "validTypes" to RecommendationType.values().map { it.name }
//            )
//            return ResponseEntity.badRequest().body(errorResponse)
//        }
//
//        val stats = recommendationService.getExclusionStatistics(member, recommendationType)
//
//        log.info {
//            "제외 통계 조회 API 응답 - userId: ${member.getIdOrThrow()}, " +
//            "totalExcluded: ${(stats["excludedCounts"] as? Map<*, *>)?.get("total")}"
//        }
//
//        return ResponseEntity.ok(stats)
//    }

    // ===== 기존 MemberController API 호환성을 위한 Deprecated 엔드포인트들 =====

//    @GetMapping("/legacy/recommend")
//    @Deprecated("Use GET /api/v1/recommendations/daily-code-matching instead", ReplaceWith("getDailyCodeMatching"))
//    override fun legacyRecommendMembers(
//        @LoginMember member: Member
//    ): ResponseEntity<MemberRecommendResponse> {
//        log.warn { "Deprecated API 호출 - /api/v1/recommendations/legacy/recommend - userId: ${member.getIdOrThrow()}" }
//
//        // Deprecated API이므로 빈 응답 반환하고 새로운 API 사용을 권장
//        return ResponseEntity.ok(MemberRecommendResponse(emptyList()))
//    }

//    @GetMapping("/legacy/all")
//    @Deprecated("Use GET /api/v1/recommendations/random instead", ReplaceWith("getRandomMembers"))
//    override fun legacyGetRandomMembers(
//        @LoginMember member: Member,
//        @RequestParam(defaultValue = "0") page: Int,
//        @RequestParam(defaultValue = "8") size: Int
//    ): ResponseEntity<Page<FullProfileResponse>> {
//        log.warn {
//            "Deprecated API 호출 - /api/v1/recommendations/legacy/all - userId: ${member.getIdOrThrow()}, " +
//            "page: $page, size: $size"
//        }
//
//        // Deprecated API이므로 새로운 API로 리디렉션
//        return getRandomMembers(member, page, size)
//    }

    // ===== 새로운 통합 추천 API들 =====
}
