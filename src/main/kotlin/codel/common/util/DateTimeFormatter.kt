package codel.common.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeFormatter {
    // 지역별 시간대 매핑
    private val ZONE_MAP = mapOf(
        "ko" to ZoneId.of("Asia/Seoul"),
        "en" to ZoneId.of("UTC"),
        "ja" to ZoneId.of("Asia/Tokyo")
    )

    // 지역별 날짜 포맷터 매핑
    private val DATE_FORMATTER_MAP = mapOf(
        "ko" to DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"),
        "en" to DateTimeFormatter.ofPattern("MMM dd, yyyy"),
        "ja" to DateTimeFormatter.ofPattern("yyyy年MM月dd日")
    )

    /**
     * 지역에 맞는 시간대를 반환 (지원하지 않는 지역은 한국 시간대 반환)
     *
     * @param locale 지역 코드 (예: "ko", "en", "ja")
     * @return 해당 지역의 시간대
     */
    private fun getZoneId(locale: String): ZoneId {
        return ZONE_MAP[locale] ?: ZONE_MAP["ko"]!!
    }

    /**
     * 지역에 맞는 날짜 포맷터를 반환 (지원하지 않는 지역은 한국 포맷 반환)
     *
     * @param locale 지역 코드 (예: "ko", "en", "ja")
     * @return 해당 지역의 날짜 포맷터
     */
    private fun getDateFormatter(locale: String): DateTimeFormatter {
        return DATE_FORMATTER_MAP[locale] ?: DATE_FORMATTER_MAP["ko"]!!
    }

    /**
     * 지정된 지역의 시간대 기준으로 오늘 날짜를 해당 지역 포맷으로 반환
     *
     * @param locale 지역 코드 (기본값: "ko", 예: "ko" -> "2025년 10월 28일", "en" -> "Oct 28, 2025")
     * @return 지역 포맷으로 변환된 오늘 날짜
     */
    fun getTodayInLocalFormat(locale: String = "ko"): String {
        val zoneId = getZoneId(locale)
        val formatter = getDateFormatter(locale)
        val today = LocalDate.now(zoneId)
        return today.format(formatter)
    }

    /**
     * 주어진 LocalDate를 지역 포맷으로 변환
     *
     * @param date 변환할 날짜
     * @param locale 지역 코드 (기본값: "ko", 예: "ko", "en", "ja")
     * @return 지역 포맷으로 변환된 날짜
     */
    fun formatToLocal(date: LocalDate, locale: String = "ko"): String {
        val formatter = getDateFormatter(locale)
        return date.format(formatter)
    }

    /**
     * 지정된 지역의 시간대 기준으로 오늘 날짜를 LocalDate로 반환
     *
     * @param locale 지역 코드 (기본값: "ko", 예: "ko", "en", "ja")
     * @return 해당 지역 시간대의 오늘 날짜
     */
    fun getToday(locale: String = "ko"): LocalDate {
        val zoneId = getZoneId(locale)
        return LocalDate.now(zoneId)
    }

    /**
     * UTC 날짜를 지정된 지역의 시간대 날짜로 변환
     *
     * @param utcDate UTC 기준 날짜
     * @param locale 지역 코드 (기본값: "ko", 예: "ko", "en", "ja")
     * @return 변환된 날짜
     */
    fun convertUtcDateToLocale(utcDate: LocalDate, locale: String = "ko"): LocalDate {
        val targetZoneId = getZoneId(locale)
        // UTC 날짜를 자정 시간으로 변환
        val utcDateTime = utcDate.atStartOfDay(ZoneId.of("UTC"))
        // 타겟 시간대로 변환
        val targetDateTime = utcDateTime.withZoneSameInstant(targetZoneId)
        return targetDateTime.toLocalDate()
    }

    /**
     * 두 날짜가 지정된 지역 시간대 기준으로 같은 날인지 확인
     *
     * @param date1 첫 번째 날짜 (UTC 기준)
     * @param date2 두 번째 날짜 (UTC 기준)
     * @param locale 지역 코드 (기본값: "ko", 예: "ko", "en", "ja")
     * @return 같은 날이면 true, 다르면 false
     */
    fun isSameDayInLocale(date1: LocalDate, date2: LocalDate, locale: String = "ko"): Boolean {
        val convertedDate1 = convertUtcDateToLocale(date1, locale)
        val convertedDate2 = convertUtcDateToLocale(date2, locale)
        return convertedDate1 == convertedDate2
    }
}
