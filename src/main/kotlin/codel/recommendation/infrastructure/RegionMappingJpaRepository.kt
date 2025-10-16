package codel.recommendation.infrastructure

import codel.recommendation.domain.RegionMapping
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

/**
 * 지역 매핑 관리 Repository
 * 버킷 정책에서 사용할 지역 인접 관계 조회를 담당
 */
interface RegionMappingJpaRepository : JpaRepository<RegionMapping, Long> {
    
    /**
     * 특정 메인 지역의 인접 지역들을 우선순위 순으로 조회
     * 버킷 정책 B3 단계에서 사용
     * 
     * @param mainRegion 기준 메인 지역 (예: "서울")
     * @return 우선순위 순으로 정렬된 인접 지역 리스트 (예: ["경기", "인천"])
     */
    @Query("""
        SELECT rm.adjacentRegion 
        FROM RegionMapping rm 
        WHERE rm.mainRegion = :mainRegion 
        ORDER BY rm.priorityOrder ASC
    """)
    fun findAdjacentRegionsByPriority(@Param("mainRegion") mainRegion: String): List<String>
    
    /**
     * 모든 메인 지역과 인접 지역 매핑을 우선순위와 함께 조회
     * 전체 지역 관계 파악 및 관리용
     */
    @Query("""
        SELECT rm.mainRegion, rm.adjacentRegion, rm.priorityOrder 
        FROM RegionMapping rm 
        ORDER BY rm.mainRegion, rm.priorityOrder ASC
    """)
    fun findAllRegionMappingsWithPriority(): List<Array<Any>>
    
    /**
     * 특정 메인 지역의 모든 매핑 정보 조회
     * 지역별 관계 상세 조회 시 사용
     */
    fun findByMainRegionOrderByPriorityOrder(mainRegion: String): List<RegionMapping>
    
    /**
     * 특정 지역이 인접 지역으로 등록된 모든 매핑 조회
     * 역방향 관계 조회 (예: "경기"가 어떤 지역들의 인접 지역인지)
     */
    fun findByAdjacentRegionOrderByPriorityOrder(adjacentRegion: String): List<RegionMapping>
    
    /**
     * 특정 지역 쌍의 매핑 존재 여부 확인
     * 중복 등록 방지 및 관계 확인
     */
    fun existsByMainRegionAndAdjacentRegion(mainRegion: String, adjacentRegion: String): Boolean
    
    /**
     * 특정 메인 지역의 매핑 개수 조회
     * 지역별 인접 관계 수 확인
     */
    fun countByMainRegion(mainRegion: String): Long
    
    /**
     * 모든 메인 지역 목록 조회 (중복 제거)
     * 지역 선택 드롭다운 등에서 사용
     */
    @Query("""
        SELECT DISTINCT rm.mainRegion 
        FROM RegionMapping rm 
        ORDER BY rm.mainRegion ASC
    """)
    fun findDistinctMainRegions(): List<String>
    
    /**
     * 모든 인접 지역 목록 조회 (중복 제거)
     * 전체 지역 목록 파악용
     */
    @Query("""
        SELECT DISTINCT rm.adjacentRegion 
        FROM RegionMapping rm 
        ORDER BY rm.adjacentRegion ASC
    """)
    fun findDistinctAdjacentRegions(): List<String>
    
    /**
     * 특정 메인 지역의 N번째까지 인접 지역 조회
     * 버킷 정책에서 제한된 개수만 필요할 때 사용
     */
    @Query("""
        SELECT rm.adjacentRegion 
        FROM RegionMapping rm 
        WHERE rm.mainRegion = :mainRegion 
        AND rm.priorityOrder <= :maxPriority
        ORDER BY rm.priorityOrder ASC
    """)
    fun findAdjacentRegionsByMaxPriority(
        @Param("mainRegion") mainRegion: String,
        @Param("maxPriority") maxPriority: Int
    ): List<String>
    
    /**
     * 지역별 매핑 통계 조회
     * 관리자 페이지에서 지역 관계 현황 파악용
     */
    @Query("""
        SELECT rm.mainRegion, COUNT(rm) as mappingCount
        FROM RegionMapping rm 
        GROUP BY rm.mainRegion
        ORDER BY mappingCount DESC, rm.mainRegion ASC
    """)
    fun getRegionMappingStatistics(): List<Array<Any>>
    
    /**
     * 특정 메인 지역의 매핑 삭제
     * 지역 관계 재정의 시 사용
     */
    fun deleteByMainRegion(mainRegion: String): Long
    
    /**
     * 특정 지역 쌍의 매핑 삭제
     * 개별 관계 제거 시 사용
     */
    fun deleteByMainRegionAndAdjacentRegion(mainRegion: String, adjacentRegion: String): Long
    
    /**
     * 버킷 정책 B3를 위한 확장된 인접 지역 조회
     * 1차, 2차 인접 지역까지 확장하여 조회
     */
    @Query("""
        SELECT DISTINCT rm2.adjacentRegion 
        FROM RegionMapping rm1 
        JOIN RegionMapping rm2 ON rm1.adjacentRegion = rm2.mainRegion
        WHERE rm1.mainRegion = :mainRegion 
        AND rm2.adjacentRegion != :mainRegion
        ORDER BY rm2.adjacentRegion ASC
    """)
    fun findSecondDegreeAdjacentRegions(@Param("mainRegion") mainRegion: String): List<String>
    
    /**
     * 지역 간 최단 인접 거리 계산
     * 두 지역이 직접 인접한지, 1단계 건너뛰는지 확인
     */
    @Query("""
        SELECT CASE 
            WHEN EXISTS (
                SELECT 1 FROM RegionMapping rm 
                WHERE rm.mainRegion = :fromRegion 
                AND rm.adjacentRegion = :toRegion
            ) THEN 1
            WHEN EXISTS (
                SELECT 1 FROM RegionMapping rm1 
                JOIN RegionMapping rm2 ON rm1.adjacentRegion = rm2.mainRegion
                WHERE rm1.mainRegion = :fromRegion 
                AND rm2.adjacentRegion = :toRegion
            ) THEN 2
            ELSE 999
        END
    """)
    fun findRegionDistance(
        @Param("fromRegion") fromRegion: String, 
        @Param("toRegion") toRegion: String
    ): Int
    
    /**
     * 특정 지역에서 접근 가능한 모든 지역 조회 (1-2차 인접 포함)
     * 버킷 B3에서 확장된 후보군 생성 시 사용
     */
    @Query("""
        SELECT rm.adjacentRegion, rm.priorityOrder, 1 as degree
        FROM RegionMapping rm 
        WHERE rm.mainRegion = :mainRegion 
        
        UNION 
        
        SELECT rm2.adjacentRegion, (rm1.priorityOrder * 10 + rm2.priorityOrder) as priorityOrder, 2 as degree
        FROM RegionMapping rm1 
        JOIN RegionMapping rm2 ON rm1.adjacentRegion = rm2.mainRegion
        WHERE rm1.mainRegion = :mainRegion 
        AND rm2.adjacentRegion != :mainRegion
        
        ORDER BY degree ASC, priorityOrder ASC
    """)
    fun findAllAccessibleRegions(@Param("mainRegion") mainRegion: String): List<Array<Any>>
}

