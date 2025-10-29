package codel.recommendation.business

import java.time.*

class TimeSlotCalculator(zone: String) {

    private val zoneId: ZoneId = when(zone.lowercase()){
        "ko", "kst" -> ZoneId.of("Asia/Seoul")
        "utc" -> ZoneOffset.UTC
        else -> throw IllegalArgumentException("지원하지 않는 타임 존 : $zone")
    }

    fun getCurrentTimeSlot(): String {
        val now = LocalTime.now(zoneId)
        val hour = now.hour

        return if (hour in 10..21) {
            "10:00"
        } else {
            "22:00"
        }
    }

    /**
     * 현재 시각을 ZonedDateTime으로 반환
     */
    fun now(): ZonedDateTime = ZonedDateTime.now(zoneId)

    /**
     * 현재 날짜를 LocalDate로 반환
     */
    fun today(): LocalDate = LocalDate.now(zoneId)

    /**
     * 주어진 시간대("10:00" 또는 "22:00")의 유효 기간을 반환합니다.
     */
    fun getValidRangeFor(timeSlot: String): Pair<LocalDateTime, LocalDateTime> {
        val now = ZonedDateTime.now(zoneId)
        val today = now.toLocalDate()

        return when (timeSlot) {
            "10:00" -> {
                val start = LocalDateTime.of(today, LocalTime.of(10, 0))
                val end = LocalDateTime.of(today, LocalTime.of(22, 0))
                start to end
            }

            "22:00" -> {
                val start = if (now.hour >= 22)
                    LocalDateTime.of(today, LocalTime.of(22, 0))
                else
                    LocalDateTime.of(today.minusDays(1), LocalTime.of(22, 0))

                val end = start.plusHours(12)
                start to end
            }

            else -> throw IllegalArgumentException("잘못된 시간대: $timeSlot")
        }
    }
}
