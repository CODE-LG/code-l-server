package codel.recommendation.infrastructure

import codel.recommendation.domain.RecommendationConfigEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RecommendationConfigRepository : JpaRepository<RecommendationConfigEntity, Long> {
    
    /**
     * 설정은 단일 레코드로 관리 (ID=1)
     */
    fun findTopByOrderByIdAsc(): RecommendationConfigEntity?
}
