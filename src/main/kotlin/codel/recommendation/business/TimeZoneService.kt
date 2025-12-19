package codel.recommendation.business

import codel.config.Loggable
import org.springframework.stereotype.Service
import java.time.*

/**
 * 타임존 기반 시간 계산 서비스
 * 
 * 주요 기능:
 * - 타임존 기반 날짜/시간 계산
 * - UTC로 저장된 시간을 타임존으로 변환
 * 
 * 향후 확장:
 * - HTTP 헤더 'X-Timezone'에서 타임존 정보를 받아 처리
 */
@Service
class TimeZoneService : Loggable {
    
    companion object {
        // 한국 타임존
        private val KST = ZoneId.of("Asia/Seoul")
        
        // 기본 타임존 (한국)
        private val DEFAULT_ZONE = KST
    }
    
    /**
     * 타임존을 반환합니다.
     * 
     * 현재는 한국(KST) 고정이지만, 향후 HTTP 헤더에서 타임존을 받아 처리할 예정입니다.
     * 
     * @param timeZoneId 타임존 ID (예: "Asia/Seoul", "America/New_York")
     * @return 타임존 (기본값: Asia/Seoul)
     */
    fun getTimeZone(timeZoneId: String? = null): ZoneId {
        return timeZoneId?.let { 
            try {
                ZoneId.of(it)
            } catch (e: Exception) {
                log.warn { "Invalid timezone: $it, using default: $DEFAULT_ZONE" }
                DEFAULT_ZONE
            }
        } ?: DEFAULT_ZONE
    }
    
    /**
     * 타임존 기준으로 현재 날짜를 반환합니다.
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 타임존 기준 오늘 날짜
     */
    fun getToday(timeZoneId: String? = null): LocalDate {
        val zone = getTimeZone(timeZoneId)
        return LocalDate.now(zone)
    }
    
    /**
     * 타임존 기준으로 현재 시각을 반환합니다.
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 타임존 기준 현재 시각
     */
    fun getNow(timeZoneId: String? = null): LocalDateTime {
        val zone = getTimeZone(timeZoneId)
        return LocalDateTime.now(zone)
    }
    
    /**
     * 타임존 기준으로 오늘 자정(00:00)의 UTC 시각을 반환합니다.
     * 
     * 예: 타임존이 KST이고 2025-10-29일 경우
     * → 2025-10-29 00:00 KST = 2025-10-28 15:00 UTC
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 타임존 기준 오늘 자정의 UTC LocalDateTime
     */
    fun getTodayStartInUTC(timeZoneId: String? = null): LocalDateTime {
        val zone = getTimeZone(timeZoneId)
        val today = LocalDate.now(zone)
        
        // 타임존의 오늘 자정
        val midnight = today.atStartOfDay(zone)
        
        // UTC로 변환
        return midnight
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
    }
    
    /**
     * 타임존 기준으로 내일 자정(00:00)의 UTC 시각을 반환합니다.
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 타임존 기준 내일 자정의 UTC LocalDateTime
     */
    fun getTomorrowStartInUTC(timeZoneId: String? = null): LocalDateTime {
        val zone = getTimeZone(timeZoneId)
        val tomorrow = LocalDate.now(zone).plusDays(1)
        
        // 타임존의 내일 자정
        val midnight = tomorrow.atStartOfDay(zone)
        
        // UTC로 변환
        return midnight
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
    }
    
    /**
     * 타임존 기준으로 특정 시간대(예: "10:00", "22:00")의 UTC 시작/종료 시각을 반환합니다.
     * 
     * 로직:
     * - 10:00 슬롯: 오늘 10:00 ~ 오늘 22:00
     * - 22:00 슬롯: 오늘 22:00 ~ 내일 10:00
     * 
     * @param timeSlot 시간대 ("10:00" 또는 "22:00")
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return Pair<시작시각 UTC, 종료시각 UTC>
     */
    fun getTimeSlotRangeInUTC(timeSlot: String, timeZoneId: String? = null): Pair<LocalDateTime, LocalDateTime> {
        val zone = getTimeZone(timeZoneId)
        val now = LocalDateTime.now(zone)
        val today = now.toLocalDate()
        
        return when (timeSlot) {
            "10:00" -> {
                // 오늘 10:00 ~ 오늘 22:00
                val start = today.atTime(10, 0).atZone(zone)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
                
                val end = today.atTime(22, 0).atZone(zone)
                    .withZoneSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime()
                
                Pair(start, end)
            }
            "22:00" -> {
                val currentHour = now.hour
                
                if (currentHour >= 22) {
                    // 현재 22시 이후 → 오늘 22:00 ~ 내일 10:00
                    val start = today.atTime(22, 0).atZone(zone)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                    
                    val end = today.plusDays(1).atTime(10, 0).atZone(zone)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                    
                    Pair(start, end)
                } else {
                    // 현재 22시 이전 (0~21시) → 어제 22:00 ~ 오늘 10:00
                    val start = today.minusDays(1).atTime(22, 0).atZone(zone)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                    
                    val end = today.atTime(10, 0).atZone(zone)
                        .withZoneSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime()
                    
                    Pair(start, end)
                }
            }
            else -> throw IllegalArgumentException("Invalid timeSlot: $timeSlot")
        }
    }
    
    /**
     * 타임존 기준으로 현재 활성 시간대를 반환합니다.
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 현재 활성 시간대 ("10:00" 또는 "22:00")
     */
    fun getCurrentTimeSlot(timeZoneId: String? = null): String {
        val zone = getTimeZone(timeZoneId)
        val now = LocalDateTime.now(zone)
        val currentHour = now.hour
        
        return if (currentHour in 10..21) {
            "10:00"  // 10시 ~ 21시 59분 → 10시 추천
        } else {
            "22:00"  // 22시 ~ 다음날 9시 59분 → 22시 추천
        }
    }
    
    /**
     * UTC로 저장된 시각을 지정된 타임존으로 변환합니다.
     * 
     * @param utcDateTime UTC 시각
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return 타임존의 시각
     */
    fun convertUTCToZone(utcDateTime: LocalDateTime, timeZoneId: String? = null): LocalDateTime {
        val zone = getTimeZone(timeZoneId)
        
        return utcDateTime
            .atZone(ZoneOffset.UTC)
            .withZoneSameInstant(zone)
            .toLocalDateTime()
    }
    
    /**
     * 타임존의 시각을 UTC로 변환합니다.
     * 
     * @param dateTime 타임존의 시각
     * @param timeZoneId 타임존 ID (null이면 기본값 사용)
     * @return UTC 시각
     */
    fun convertZoneToUTC(dateTime: LocalDateTime, timeZoneId: String? = null): LocalDateTime {
        val zone = getTimeZone(timeZoneId)
        
        return dateTime
            .atZone(zone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
    }
}
