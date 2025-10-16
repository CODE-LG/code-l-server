package codel.recommendation.business

import codel.config.Loggable
import codel.recommendation.domain.RecommendationConfigEntity
import codel.recommendation.infrastructure.RecommendationConfigRepository
import jakarta.annotation.PostConstruct
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 추천 시스템 설정 관리 Service
 * 
 * DB에서 설정을 조회하고 런타임에 변경 가능하도록 관리
 * 캐싱을 통해 성능 최적화
 */
@Service
@Transactional(readOnly = true)
class RecommendationConfigService(
    private val configRepository: RecommendationConfigRepository
) : Loggable {
    
    /**
     * 애플리케이션 시작 시 기본 설정 초기화
     */
    @PostConstruct
    @Transactional
    fun initializeDefaultConfig() {
        val existingConfig = configRepository.findTopByOrderByIdAsc()
        
        if (existingConfig == null) {
            val defaultConfig = RecommendationConfigEntity.createDefault()
            configRepository.save(defaultConfig)
            log.info { "추천 시스템 기본 설정 초기화 완료" }
        } else {
            log.info { "추천 시스템 설정 로드 완료 - dailyCodeCount: ${existingConfig.dailyCodeCount}, codeTimeCount: ${existingConfig.codeTimeCount}" }
        }
    }
    
    /**
     * 현재 설정 조회 (캐시됨)
     */
    @Cacheable(value = ["recommendationConfig"], key = "'singleton'")
    fun getConfig(): RecommendationConfigEntity {
        return configRepository.findTopByOrderByIdAsc()
            ?: RecommendationConfigEntity.createDefault().also {
                log.warn { "설정을 찾을 수 없어 기본값 반환" }
            }
    }
    
    /**
     * 오늘의 코드매칭 추천 인원 수 조회
     */
    fun getDailyCodeCount(): Int {
        return getConfig().dailyCodeCount
    }
    
    /**
     * 코드타임 추천 인원 수 조회
     */
    fun getCodeTimeCount(): Int {
        return getConfig().codeTimeCount
    }
    
    /**
     * 코드타임 시간대 목록 조회
     */
    fun getCodeTimeSlots(): List<String> {
        return getConfig().getCodeTimeSlotsAsList()
    }
    
    /**
     * 오늘의 코드매칭 갱신 시점 조회
     */
    fun getDailyRefreshTime(): String {
        return getConfig().dailyRefreshTime
    }
    
    /**
     * 중복 방지 기간 조회
     */
    fun getRepeatAvoidDays(): Int {
        return getConfig().repeatAvoidDays
    }
    
    /**
     * 중복 허용 여부 조회
     */
    fun getAllowDuplicate(): Boolean {
        return getConfig().allowDuplicate
    }
    
    /**
     * 설정 업데이트 (캐시 제거)
     */
    @Transactional
    @CacheEvict(value = ["recommendationConfig"], key = "'singleton'")
    fun updateConfig(
        dailyCodeCount: Int? = null,
        codeTimeCount: Int? = null,
        codeTimeSlots: List<String>? = null,
        dailyRefreshTime: String? = null,
        repeatAvoidDays: Int? = null,
        allowDuplicate: Boolean? = null
    ): RecommendationConfigEntity {
        val config = configRepository.findTopByOrderByIdAsc()
            ?: RecommendationConfigEntity.createDefault().also { configRepository.save(it) }
        
        dailyCodeCount?.let { 
            require(it > 0) { "dailyCodeCount는 0보다 커야 합니다" }
            config.dailyCodeCount = it 
        }
        codeTimeCount?.let { 
            require(it > 0) { "codeTimeCount는 0보다 커야 합니다" }
            config.codeTimeCount = it 
        }
        codeTimeSlots?.let { 
            require(it.isNotEmpty()) { "codeTimeSlots는 비어있을 수 없습니다" }
            config.setCodeTimeSlotsFromList(it) 
        }
        dailyRefreshTime?.let { 
            require(it.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                "잘못된 시간 형식: $it"
            }
            config.dailyRefreshTime = it 
        }
        repeatAvoidDays?.let { 
            require(it >= 0) { "repeatAvoidDays는 0 이상이어야 합니다" }
            config.repeatAvoidDays = it 
        }
        allowDuplicate?.let { config.allowDuplicate = it }
        
        val updated = configRepository.save(config)
        log.info { "추천 시스템 설정 업데이트 완료" }
        
        return updated
    }
    
    /**
     * 설정을 Map으로 반환 (API 응답용)
     */
    fun getConfigAsMap(): Map<String, Any> {
        val config = getConfig()
        return mapOf(
            "dailyCodeCount" to config.dailyCodeCount,
            "codeTimeCount" to config.codeTimeCount,
            "codeTimeSlots" to config.getCodeTimeSlotsAsList(),
            "dailyRefreshTime" to config.dailyRefreshTime,
            "repeatAvoidDays" to config.repeatAvoidDays,
            "allowDuplicate" to config.allowDuplicate
        )
    }
}
