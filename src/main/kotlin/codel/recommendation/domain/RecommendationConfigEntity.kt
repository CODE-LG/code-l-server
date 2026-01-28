package codel.recommendation.domain

import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 추천 시스템 설정 Entity
 * 
 * 런타임에 변경 가능한 추천 시스템 설정값을 DB에 저장
 * 단일 레코드로 관리 (ID=1 고정)
 */
@Entity
@Table(name = "recommendation_config")
class RecommendationConfigEntity(
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    /**
     * 오늘의 코드매칭 추천 인원 수
     */
    @Column(nullable = false)
    var dailyCodeCount: Int = 3,
    
    /**
     * 코드타임 추천 인원 수
     */
    @Column(nullable = false)
    var codeTimeCount: Int = 2,
    
    /**
     * 코드타임 시간대 목록 (쉼표로 구분, 예: "10:00,22:00")
     */
    @Column(nullable = false, length = 500)
    var codeTimeSlots: String = "10:00,22:00",
    
    /**
     * 오늘의 코드매칭 갱신 시점
     */
    @Column(nullable = false, length = 5)
    var dailyRefreshTime: String = "00:00",
    
    /**
     * 동일 인연 재노출 금지 기간 (일 단위)
     */
    @Column(nullable = false)
    var repeatAvoidDays: Int = 3,
    
    /**
     * 오늘의 코드매칭과 코드타임 간 중복 허용 여부
     */
    @Column(nullable = false)
    var allowDuplicate: Boolean = true,

    /**
     * 우선 추천 최대 나이 차이 (0~N살)
     * 이 범위 내의 후보가 우선 추천됨
     */
    @Column(nullable = false)
    var agePreferredMaxDiff: Int = 5,

    /**
     * 컷오프 기준 나이 차이
     * 이 값 이상의 나이 차이는 기본적으로 추천에서 제외
     */
    @Column(nullable = false)
    var ageCutoffDiff: Int = 6,

    /**
     * 후보 부족 시 컷오프 대상 허용 여부
     * true: 0~5살 후보가 부족하면 6살 이상도 추천
     * false: 0~5살 후보만 추천 (부족해도 6살 이상 제외)
     */
    @Column(nullable = false)
    var ageAllowCutoffWhenInsufficient: Boolean = true,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
    
) {
    
    @PreUpdate
    fun onUpdate() {
        updatedAt = LocalDateTime.now()
    }
    
    /**
     * codeTimeSlots를 List로 변환
     */
    fun getCodeTimeSlotsAsList(): List<String> {
        return codeTimeSlots.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
    
    /**
     * List를 codeTimeSlots 문자열로 변환
     */
    fun setCodeTimeSlotsFromList(slots: List<String>) {
        codeTimeSlots = slots.joinToString(",")
    }
    
    companion object {
        /**
         * 기본 설정 생성
         */
        fun createDefault(): RecommendationConfigEntity {
            return RecommendationConfigEntity(
                id = 1L,
                dailyCodeCount = 3,
                codeTimeCount = 2,
                codeTimeSlots = "10:00,22:00",
                dailyRefreshTime = "00:00",
                repeatAvoidDays = 3,
                allowDuplicate = true,
                agePreferredMaxDiff = 5,
                ageCutoffDiff = 6,
                ageAllowCutoffWhenInsufficient = true
            )
        }
    }
}
