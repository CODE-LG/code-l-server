package codel.recommendation.business

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.*

@DisplayName("TimeZoneService 테스트")
class TimeZoneServiceTest {

    private val timeZoneService = TimeZoneService()

    @Test
    @DisplayName("기본 타임존은 Asia/Seoul이다")
    fun getDefaultTimeZone() {
        // when
        val zone = timeZoneService.getTimeZone()

        // then
        assertEquals(ZoneId.of("Asia/Seoul"), zone)
    }

    @Test
    @DisplayName("유효한 타임존 ID를 받으면 해당 타임존을 반환한다")
    fun getValidTimeZone() {
        // when
        val nyZone = timeZoneService.getTimeZone("America/New_York")
        val londonZone = timeZoneService.getTimeZone("Europe/London")

        // then
        assertEquals(ZoneId.of("America/New_York"), nyZone)
        assertEquals(ZoneId.of("Europe/London"), londonZone)
    }

    @Test
    @DisplayName("유효하지 않은 타임존 ID를 받으면 기본 타임존을 반환한다")
    fun getInvalidTimeZone() {
        // when
        val zone = timeZoneService.getTimeZone("Invalid/Zone")

        // then
        assertEquals(ZoneId.of("Asia/Seoul"), zone)
    }

    @Test
    @DisplayName("KST 기준 오늘 자정의 UTC 시각을 반환한다")
    fun getTodayStartInUTC_KST() {
        // given
        val kstZone = ZoneId.of("Asia/Seoul")
        val kstToday = LocalDate.now(kstZone)
        val expectedUTC = kstToday.atStartOfDay(kstZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()

        // when
        val result = timeZoneService.getTodayStartInUTC()

        // then
        assertEquals(expectedUTC.toLocalDate(), result.toLocalDate())
        assertEquals(expectedUTC.hour, result.hour)
    }

    @Test
    @DisplayName("뉴욕 기준 오늘 자정의 UTC 시각을 반환한다")
    fun getTodayStartInUTC_NewYork() {
        // given
        val nyZone = ZoneId.of("America/New_York")
        val nyToday = LocalDate.now(nyZone)
        val expectedUTC = nyToday.atStartOfDay(nyZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()

        // when
        val result = timeZoneService.getTodayStartInUTC("America/New_York")

        // then
        assertEquals(expectedUTC.toLocalDate(), result.toLocalDate())
        assertEquals(expectedUTC.hour, result.hour)
    }

    @Test
    @DisplayName("KST 기준 10시 슬롯의 UTC 시간 범위를 반환한다")
    fun getTimeSlotRangeInUTC_10AM_KST() {
        // given
        val kstZone = ZoneId.of("Asia/Seoul")
        val kstToday = LocalDate.now(kstZone)
        
        val expectedStart = kstToday.atTime(10, 0).atZone(kstZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()
        
        val expectedEnd = kstToday.atTime(22, 0).atZone(kstZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()

        // when
        val (start, end) = timeZoneService.getTimeSlotRangeInUTC("10:00")

        // then
        assertEquals(expectedStart, start)
        assertEquals(expectedEnd, end)
    }

    @Test
    @DisplayName("KST 기준 22시 슬롯의 UTC 시간 범위를 반환한다 - 현재가 22시 이후인 경우")
    fun getTimeSlotRangeInUTC_10PM_KST_After22() {
        // given
        val kstZone = ZoneId.of("Asia/Seoul")
        val kstNow = LocalDateTime.now(kstZone)
        
        // 22시 이후인지 확인
        if (kstNow.hour >= 22) {
            val kstToday = kstNow.toLocalDate()
            
            val expectedStart = kstToday.atTime(22, 0).atZone(kstZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
            
            val expectedEnd = kstToday.plusDays(1).atTime(10, 0).atZone(kstZone)
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()

            // when
            val (start, end) = timeZoneService.getTimeSlotRangeInUTC("22:00")

            // then
            assertEquals(expectedStart, start)
            assertEquals(expectedEnd, end)
        }
    }

    @Test
    @DisplayName("KST 기준 현재 활성 시간대를 반환한다 - 10시~21시는 10:00 슬롯")
    fun getCurrentTimeSlot_Morning_KST() {
        // given
        val kstZone = ZoneId.of("Asia/Seoul")
        val kstNow = LocalDateTime.now(kstZone)
        val currentHour = kstNow.hour

        // when
        val result = timeZoneService.getCurrentTimeSlot()

        // then
        if (currentHour in 10..21) {
            assertEquals("10:00", result)
        } else {
            assertEquals("22:00", result)
        }
    }

    @Test
    @DisplayName("다양한 타임존에서 현재 시간대를 올바르게 반환한다")
    fun getCurrentTimeSlot_VariousTimezones() {
        // 뉴욕
        val nyTimeSlot = timeZoneService.getCurrentTimeSlot("America/New_York")
        assertTrue(nyTimeSlot in listOf("10:00", "22:00"))

        // 런던
        val londonTimeSlot = timeZoneService.getCurrentTimeSlot("Europe/London")
        assertTrue(londonTimeSlot in listOf("10:00", "22:00"))

        // 도쿄
        val tokyoTimeSlot = timeZoneService.getCurrentTimeSlot("Asia/Tokyo")
        assertTrue(tokyoTimeSlot in listOf("10:00", "22:00"))
    }

    @Test
    @DisplayName("UTC 시각을 KST로 변환한다")
    fun convertUTCToZone_KST() {
        // given
        val utcDateTime = LocalDateTime.of(2025, 10, 28, 15, 0) // UTC
        val kstZone = ZoneId.of("Asia/Seoul")
        
        val expected = utcDateTime.atZone(ZoneOffset.UTC)
            .withZoneSameInstant(kstZone)
            .toLocalDateTime()

        // when
        val result = timeZoneService.convertUTCToZone(utcDateTime)

        // then
        assertEquals(expected, result)
        assertEquals(LocalDateTime.of(2025, 10, 29, 0, 0), result) // KST는 UTC+9
    }

    @Test
    @DisplayName("KST 시각을 UTC로 변환한다")
    fun convertZoneToUTC_KST() {
        // given
        val kstDateTime = LocalDateTime.of(2025, 10, 29, 0, 0) // KST 자정
        val kstZone = ZoneId.of("Asia/Seoul")
        
        val expected = kstDateTime.atZone(kstZone)
            .withZoneSameInstant(ZoneOffset.UTC)
            .toLocalDateTime()

        // when
        val result = timeZoneService.convertZoneToUTC(kstDateTime)

        // then
        assertEquals(expected, result)
        assertEquals(LocalDateTime.of(2025, 10, 28, 15, 0), result) // UTC는 KST-9
    }

    @Test
    @DisplayName("22시 슬롯은 날짜를 넘어가는 시간 범위를 반환한다")
    fun getTimeSlotRangeInUTC_22PM_CrossesDate() {
        // given & when
        val (start, end) = timeZoneService.getTimeSlotRangeInUTC("22:00")

        // then
        // 22시 슬롯은 최소 12시간 (22:00 ~ 다음날 10:00)
        val duration = Duration.between(start, end)
        assertEquals(12, duration.toHours())
    }

    @Test
    @DisplayName("잘못된 시간대 슬롯을 입력하면 예외가 발생한다")
    fun getTimeSlotRangeInUTC_InvalidSlot() {
        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            timeZoneService.getTimeSlotRangeInUTC("15:00")
        }
    }

    @Test
    @DisplayName("오늘과 내일 자정 사이는 24시간이다")
    fun getTodayAndTomorrowDifference() {
        // when
        val todayStart = timeZoneService.getTodayStartInUTC()
        val tomorrowStart = timeZoneService.getTomorrowStartInUTC()

        // then
        val duration = Duration.between(todayStart, tomorrowStart)
        assertEquals(24, duration.toHours())
    }
}
