package codel.recommendation.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDate

/**
 * 추천 이력 관리 엔티티
 * 중복 방지 및 추천 결과 추적을 위한 테이블
 */
@Entity
@Table(
    name = "recommendation_histories",
    indexes = [
        Index(name = "idx_user_recommended_date", columnList = "user_id,recommended_user_id,recommended_date"),
        Index(name = "idx_user_date", columnList = "user_id,recommended_date"),
        Index(name = "idx_recommended_date", columnList = "recommended_date")
    ]
)
class RecommendationHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 추천을 받은 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: Member,

    /**
     * 추천된 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_user_id", nullable = false)
    val recommendedUser: Member,

    /**
     * 추천된 날짜
     */
    @Column(name = "recommended_date", nullable = false)
    val recommendedDate: LocalDate,

    /**
     * 추천 타입 (오늘의 코드매칭 or 코드타임)
     */
    @Column(name = "recommendation_type", nullable = false)
    @Enumerated(EnumType.STRING)
    val recommendationType: RecommendationType,

    /**
     * 코드타임의 경우 시간대 정보 (예: "10:00", "22:00")
     * 오늘의 코드매칭의 경우 null
     */
    @Column(name = "recommendation_time_slot")
    val recommendationTimeSlot: String? = null

) : BaseTimeEntity()
