package codel.recommendation.presentation

import codel.config.Loggable
import codel.recommendation.business.RecommendationConfigService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/v1/admin/recommendation-config")
class RecommendationConfigController(
    private val configService: RecommendationConfigService
) : Loggable {
    
    @GetMapping
    fun getConfig(): ResponseEntity<Map<String, Any>> {
        log.info { "추천 시스템 설정 조회" }
        return ResponseEntity.ok(configService.getConfigAsMap())
    }
    
    @PutMapping
    fun updateConfig(@RequestBody request: UpdateConfigRequest): ResponseEntity<Map<String, Any>> {
        log.info { "추천 시스템 설정 업데이트 - request: $request" }
        
        return try {
            val updated = configService.updateConfig(
                dailyCodeCount = request.dailyCodeCount,
                codeTimeCount = request.codeTimeCount,
                codeTimeSlots = request.codeTimeSlots,
                dailyRefreshTime = request.dailyRefreshTime,
                repeatAvoidDays = request.repeatAvoidDays,
                allowDuplicate = request.allowDuplicate
            )
            
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "설정이 업데이트되었습니다",
                "config" to mapOf(
                    "dailyCodeCount" to updated.dailyCodeCount,
                    "codeTimeCount" to updated.codeTimeCount,
                    "codeTimeSlots" to updated.getCodeTimeSlotsAsList(),
                    "dailyRefreshTime" to updated.dailyRefreshTime,
                    "repeatAvoidDays" to updated.repeatAvoidDays,
                    "allowDuplicate" to updated.allowDuplicate
                )
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to (e.message ?: "잘못된 값입니다")
            ))
        }
    }
    
    @PatchMapping("/daily-code-count")
    fun updateDailyCodeCount(@RequestParam count: Int): ResponseEntity<Map<String, Any>> {
        return try {
            configService.updateConfig(dailyCodeCount = count)
            ResponseEntity.ok(mapOf("success" to true, "message" to "변경 완료"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message!!))
        }
    }
    
    @PatchMapping("/code-time-count")
    fun updateCodeTimeCount(@RequestParam count: Int): ResponseEntity<Map<String, Any>> {
        return try {
            configService.updateConfig(codeTimeCount = count)
            ResponseEntity.ok(mapOf("success" to true, "message" to "변경 완료"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("success" to false, "message" to e.message!!))
        }
    }
}

data class UpdateConfigRequest(
    val dailyCodeCount: Int? = null,
    val codeTimeCount: Int? = null,
    val codeTimeSlots: List<String>? = null,
    val dailyRefreshTime: String? = null,
    val repeatAvoidDays: Int? = null,
    val allowDuplicate: Boolean? = null
)
