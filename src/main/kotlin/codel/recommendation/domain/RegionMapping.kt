package codel.recommendation.domain

import jakarta.persistence.*

/**
 * 지역 인접 관계 매핑 엔티티
 * 버킷 정책(B3: 인접 mainRegion)에서 사용하는 지역 간 인접 관계를 관리
 */
@Entity
@Table(
    name = "region_mappings",
    indexes = [
        Index(name = "idx_main_region_priority", columnList = "main_region,priority_order")
    ]
)
class RegionMapping(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 기준이 되는 메인 지역 (예: 서울, 경기, 부산 등)
     */
    @Column(name = "main_region", nullable = false, length = 20)
    val mainRegion: String,

    /**
     * 인접한 지역 (예: 서울의 인접 지역 → 경기, 인천)
     */
    @Column(name = "adjacent_region", nullable = false, length = 20)
    val adjacentRegion: String,

    /**
     * 우선순위 (낮을수록 높은 우선순위)
     * 예: 서울 → 경기(1), 인천(2)
     */
    @Column(name = "priority_order", nullable = false)
    val priorityOrder: Int

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RegionMapping) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "RegionMapping(id=$id, mainRegion='$mainRegion', adjacentRegion='$adjacentRegion', priorityOrder=$priorityOrder)"
    }
}
