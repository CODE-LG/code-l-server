package codel.recommendation.presentation

import codel.config.argumentresolver.LoginMember
import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.business.RecommendationService
import codel.recommendation.domain.RecommendationType
import codel.recommendation.presentation.response.*
import codel.recommendation.presentation.swagger.RecommendationSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 추천 시스템 API Controller
 * 
 * 새로운 지역 기반 버킷 정책 + 시간대별 추천 시스템 제공
 * 기존 랜덤 추천을 대체하는 고도화된 추천 서비스
 */
@RestController
@RequestMapping("/api/v1/recommendations")
class RecommendationController(
    private val recommendationService: RecommendationService
) : RecommendationSwagger, Loggable {
    
    @GetMapping
    override fun getRecommendation(
        @LoginMember member: Member,
        @RequestParam(defaultValue = "false") preferCodeTime: Boolean
    ): ResponseEntity<RecommendationResponse> {
        log.info { "통합 추천 API 호출 - userId: ${member.getIdOrThrow()}, preferCodeTime: $preferCodeTime" }
        
        val result = recommendationService.getRecommendation(member, preferCodeTime)
        val response = RecommendationResponse.from(result)
        
        log.info { 
            "통합 추천 API 응답 - userId: ${member.getIdOrThrow()}, " +
            "primary: ${result.primaryRecommendation}, " +
            "dailyCount: ${result.dailyCodeMatching.size}, " +
            "codeTimeCount: ${result.codeTimeResult?.recommendationCount ?: 0}" 
        }
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/daily-code-matching")
    override fun getDailyCodeMatching(
        @LoginMember member: Member
    ): ResponseEntity<DailyCodeMatchingResponse> {
        log.info { "오늘의 코드매칭 API 호출 - userId: ${member.getIdOrThrow()}" }
        
        val members = recommendationService.getDailyCodeMatching(member)
        val response = DailyCodeMatchingResponse.from(members)
        
        log.info { "오늘의 코드매칭 API 응답 - userId: ${member.getIdOrThrow()}, count: ${members.size}" }
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/code-time")
    override fun getCodeTime(
        @LoginMember member: Member
    ): ResponseEntity<CodeTimeResponse> {
        log.info { "코드타임 API 호출 - userId: ${member.getIdOrThrow()}" }
        
        val result = recommendationService.getCodeTime(member)
        val response = CodeTimeResponse.from(result)
        
        log.info { 
            "코드타임 API 응답 - userId: ${member.getIdOrThrow()}, " +
            "timeSlot: ${result.timeSlot}, active: ${result.isActiveTime}, " +
            "count: ${result.recommendationCount}" 
        }
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/code-time/{timeSlot}")
    override fun getCodeTimeBySlot(
        @LoginMember member: Member,
        @PathVariable timeSlot: String
    ): ResponseEntity<CodeTimeResponse> {
        log.info { "특정 시간대 코드타임 API 호출 - userId: ${member.getIdOrThrow()}, timeSlot: $timeSlot" }
        
        val result = recommendationService.getCodeTimeBySlot(member, timeSlot)
        val response = CodeTimeResponse.from(result)
        
        log.info { 
            "특정 시간대 코드타임 API 응답 - userId: ${member.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, count: ${result.recommendationCount}" 
        }
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/overview")
    override fun getRecommendationOverview(
        @LoginMember member: Member
    ): ResponseEntity<RecommendationOverviewResponse> {
        log.info { "추천 현황 종합 조회 API 호출 - userId: ${member.getIdOrThrow()}" }
        
        val overview = recommendationService.getRecommendationOverview(member)
        val response = RecommendationOverviewResponse.from(overview)
        
        log.info { 
            "추천 현황 종합 조회 API 응답 - userId: ${member.getIdOrThrow()}, " +
            "hasDaily: ${overview.hasDailyCodeMatching}, " +
            "isCodeTimeActive: ${overview.isCodeTimeActive}" 
        }
        
        return ResponseEntity.ok(response)
    }
    
    @GetMapping("/settings")
    override fun getRecommendationSettings(): ResponseEntity<Map<String, Any>> {
        log.info { "추천 시스템 설정 조회 API 호출" }
        
        val settings = recommendationService.getRecommendationSettings()
        
        log.info { "추천 시스템 설정 조회 API 응답 - settings: $settings" }
        
        return ResponseEntity.ok(settings)
    }
    
    @PostMapping("/refresh")
    override fun forceRefreshRecommendation(
        @LoginMember member: Member,
        @RequestParam type: String,
        @RequestParam(required = false) timeSlot: String?
    ): ResponseEntity<Map<String, Any>> {
        log.info { 
            "추천 강제 새로고침 API 호출 - userId: ${member.getIdOrThrow()}, " +
            "type: $type, timeSlot: $timeSlot" 
        }
        
        // String을 RecommendationType으로 변환
        val recommendationType = try {
            RecommendationType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            log.warn { "유효하지 않은 추천 타입 - userId: ${member.getIdOrThrow()}, type: $type" }
            val errorResponse: Map<String, Any> = mapOf(
                "success" to false,
                "error" to "유효하지 않은 추천 타입입니다. DAILY_CODE_MATCHING 또는 CODE_TIME을 사용하세요.",
                "validTypes" to RecommendationType.values().map { it.name }
            )
            return ResponseEntity.badRequest().body(errorResponse)
        }
        
        return try {
            val result = recommendationService.forceRefreshRecommendation(member, recommendationType, timeSlot)
            
            val response: Map<String, Any> = mapOf(
                "success" to true,
                "type" to type,
                "timeSlot" to (timeSlot ?: ""),
                "result" to result,
                "refreshedAt" to System.currentTimeMillis()
            )
            
            log.info { 
                "추천 강제 새로고침 API 응답 - userId: ${member.getIdOrThrow()}, " +
                "type: $type, success: true" 
            }
            
            ResponseEntity.ok(response)
            
        } catch (e: IllegalStateException) {
            log.warn { 
                "추천 강제 새로고침 실패 - userId: ${member.getIdOrThrow()}, " +
                "type: $type, error: ${e.message}" 
            }
            
            val errorResponse: Map<String, Any> = mapOf(
                "success" to false,
                "error" to (e.message ?: "강제 새로고침에 실패했습니다."),
                "type" to type,
                "timeSlot" to (timeSlot ?: "")
            )
            
            ResponseEntity.badRequest().body(errorResponse)
        }
    }
    
    @GetMapping("/health")
    override fun getSystemHealthCheck(): ResponseEntity<Map<String, Any>> {
        log.info { "시스템 헬스체크 API 호출" }
        
        val healthCheck = recommendationService.getSystemHealthCheck()
        
        log.info { "시스템 헬스체크 API 응답 - status: ${healthCheck["systemStatus"]}" }
        
        return ResponseEntity.ok(healthCheck)
    }
}
