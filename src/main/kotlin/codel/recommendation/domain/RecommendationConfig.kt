package codel.recommendation.domain

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * 추천 시스템 설정 클래스
 * application.yml의 recommendation 섹션과 바인딩되어 운영에서 조정 가능한 파라미터들을 관리
 */
@Component
@ConfigurationProperties(prefix = "recommendation")
data class RecommendationConfig(
    
    /**
     * 오늘의 코드매칭 추천 인원 수 (기본값: 3명)
     */
    var dailyCodeCount: Int = 3,
    
    /**
     * 코드타임 추천 인원 수 (기본값: 2명)
     */
    var codeTimeCount: Int = 2,
    
    /**
     * 코드타임 시간대 목록 (기본값: 10:00, 22:00)
     */
    var codeTimeSlots: List<String> = listOf("10:00", "22:00"),
    
    /**
     * 오늘의 코드매칭 갱신 시점 (기본값: 00:00)
     */
    var dailyRefreshTime: String = "00:00",
    
    /**
     * 동일 인연 재노출 금지 기간 (일 단위, 기본값: 3일)
     */
    var repeatAvoidDays: Int = 3,
    
    /**
     * 오늘의 코드매칭과 코드타임 간 중복 허용 여부 (기본값: true)
     */
    var allowDuplicate: Boolean = true
    
) {
    
    companion object {
        
        /**
         * 메인 지역별 인접 지역 매핑 (우선순위 순서)
         * 버킷 정책 B3에서 사용되는 인접 지역 관계 정의
         * 
         * 예시: 서울 사용자 → 경기, 인천 순으로 확장
         */
        val ADJACENT_MAIN_REGION_MAP = mapOf(
            // 수도권
            "서울" to listOf("경기", "인천"),
            "경기" to listOf("서울", "인천", "강원", "충남"),
            "인천" to listOf("서울", "경기"),
            
            // 영남권
            "부산" to listOf("울산", "경남"),
            "울산" to listOf("부산", "경남", "경북"),
            "대구" to listOf("경북", "경남"),
            
            // 호남권  
            "광주" to listOf("전남", "전북"),
            
            // 충청권
            "대전" to listOf("세종", "충남", "충북"),
            "세종" to listOf("대전", "충남", "충북"),
            
            // 강원권
            "강원" to listOf("경기", "충북", "경북"),
            
            // 충청권 확장
            "충북" to listOf("세종", "대전", "충남", "강원", "경북"),
            "충남" to listOf("세종", "대전", "경기", "전북"),
            
            // 전라권
            "전북" to listOf("전남", "충남", "경남"),
            "전남" to listOf("광주", "전북", "경남", "제주"),
            
            // 경상권
            "경북" to listOf("대구", "경남", "강원"),
            "경남" to listOf("부산", "울산", "전남", "경북"),
            
            // 제주권 (인접 지역 없음 - 전남과만 연결)
            "제주" to emptyList()
        )
        
        /**
         * 특정 메인 지역의 인접 지역 목록을 우선순위 순으로 반환
         * 
         * @param mainRegion 기준 메인 지역
         * @return 인접 지역 목록 (우선순위 순)
         */
        fun getAdjacentRegions(mainRegion: String): List<String> {
            return ADJACENT_MAIN_REGION_MAP[mainRegion] ?: emptyList()
        }
        
        /**
         * 코드타임 시간대 검증 (30분 허용 오차)
         * 
         * @param currentHour 현재 시간
         * @param currentMinute 현재 분
         * @param timeSlot 검증할 시간대 (예: "10:00")
         * @return 현재 시간이 해당 시간대에 해당하는지 여부
         */
        fun isWithinTimeSlot(currentHour: Int, currentMinute: Int, timeSlot: String): Boolean {
            val (slotHour, slotMinute) = timeSlot.split(":").map { it.toInt() }
            val currentTotalMinutes = currentHour * 60 + currentMinute
            val slotTotalMinutes = slotHour * 60 + slotMinute
            
            // 30분 허용 오차
            return kotlin.math.abs(currentTotalMinutes - slotTotalMinutes) <= 30
        }
        
        /**
         * 현재 시간에 해당하는 코드타임 시간대 반환
         * 
         * @param currentHour 현재 시간
         * @param currentMinute 현재 분
         * @param timeSlots 코드타임 시간대 목록
         * @return 해당하는 시간대 (없으면 null)
         */
        fun getCurrentTimeSlot(currentHour: Int, currentMinute: Int, timeSlots: List<String>): String? {
            return timeSlots.firstOrNull { timeSlot ->
                isWithinTimeSlot(currentHour, currentMinute, timeSlot)
            }
        }
    }
    
    /**
     * 설정 검증 메서드
     * Spring Boot 시작 시 자동으로 호출되어 잘못된 설정을 미리 검증
     */
    fun validate() {
        require(dailyCodeCount > 0) { "dailyCodeCount는 0보다 커야 합니다. 현재값: $dailyCodeCount" }
        require(codeTimeCount > 0) { "codeTimeCount는 0보다 커야 합니다. 현재값: $codeTimeCount" }
        require(repeatAvoidDays >= 0) { "repeatAvoidDays는 0 이상이어야 합니다. 현재값: $repeatAvoidDays" }
        require(codeTimeSlots.isNotEmpty()) { "codeTimeSlots는 비어있을 수 없습니다." }
        
        // 시간대 형식 검증 (HH:mm)
        codeTimeSlots.forEach { timeSlot ->
            require(timeSlot.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
                "잘못된 시간대 형식입니다: $timeSlot (올바른 형식: HH:mm)"
            }
        }
        
        // dailyRefreshTime 형식 검증
        require(dailyRefreshTime.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))) {
            "잘못된 dailyRefreshTime 형식입니다: $dailyRefreshTime (올바른 형식: HH:mm)"
        }
    }
}
