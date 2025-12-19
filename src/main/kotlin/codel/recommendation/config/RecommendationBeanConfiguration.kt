package codel.recommendation.config

import codel.recommendation.business.RecommendationConfigService
import codel.recommendation.domain.RecommendationConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 추천 시스템 Bean 설정
 * 
 * 순환 참조 방지를 위한 Bean 생성
 */
@Configuration
class RecommendationBeanConfiguration {
    
    @Bean
    fun recommendationConfig(configService: RecommendationConfigService): RecommendationConfig {
        return RecommendationConfig { configService }
    }
}
