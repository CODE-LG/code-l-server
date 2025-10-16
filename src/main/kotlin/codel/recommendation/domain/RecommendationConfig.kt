package codel.recommendation.domain

/**
 * 추천 시스템 설정 Wrapper 클래스
 * 
 * 기존 코드 호환성을 위해 RecommendationConfigService를 래핑
 * 실제 데이터는 DB(RecommendationConfigEntity)에서 조회
 * 
 * Bean으로 생성됨 (RecommendationBeanConfiguration 참조)
 */
class RecommendationConfig(
    private val configServiceProvider: () -> codel.recommendation.business.RecommendationConfigService
) {
    
    private val configService by lazy { configServiceProvider() }
    
    /**
     * 오늘의 코드매칭 추천 인원 수
     */
    val dailyCodeCount: Int
        get() = configService.getDailyCodeCount()
    
    /**
     * 코드타임 추천 인원 수
     */
    val codeTimeCount: Int
        get() = configService.getCodeTimeCount()
    
    /**
     * 코드타임 시간대 목록
     */
    val codeTimeSlots: List<String>
        get() = configService.getCodeTimeSlots()
    
    /**
     * 오늘의 코드매칭 갱신 시점
     */
    val dailyRefreshTime: String
        get() = configService.getDailyRefreshTime()
    
    /**
     * 동일 인연 재노출 금지 기간 (일 단위)
     */
    val repeatAvoidDays: Int
        get() = configService.getRepeatAvoidDays()
    
    /**
     * 오늘의 코드매칭과 코드타임 간 중복 허용 여부
     */
    val allowDuplicate: Boolean
        get() = configService.getAllowDuplicate()
    
    companion object {
        
        /**
         * 메인 지역별 인접 지역 매핑 (우선순위 순서)
         * 버킷 정책 B3에서 사용되는 인접 지역 관계 정의
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
            
            // 제주권
            "제주" to emptyList()
        )
        
        /**
         * 특정 메인 지역의 인접 지역 목록을 우선순위 순으로 반환
         */
        fun getAdjacentRegions(mainRegion: String): List<String> {
            return ADJACENT_MAIN_REGION_MAP[mainRegion] ?: emptyList()
        }
        
        /**
         * 코드타임 시간대 검증 (30분 허용 오차)
         */
        fun isWithinTimeSlot(currentHour: Int, currentMinute: Int, timeSlot: String): Boolean {
            val (slotHour, slotMinute) = timeSlot.split(":").map { it.toInt() }
            val currentTotalMinutes = currentHour * 60 + currentMinute
            val slotTotalMinutes = slotHour * 60 + slotMinute
            
            return kotlin.math.abs(currentTotalMinutes - slotTotalMinutes) <= 30
        }
        
        /**
         * 현재 시간에 해당하는 코드타임 시간대 반환
         */
        fun getCurrentTimeSlot(currentHour: Int, currentMinute: Int, timeSlots: List<String>): String? {
            return timeSlots.firstOrNull { timeSlot ->
                isWithinTimeSlot(currentHour, currentMinute, timeSlot)
            }
        }
    }
}
