package codel.kpi.batch

import codel.common.util.DateTimeFormatter
import codel.config.Loggable
import codel.kpi.business.KpiBatchService
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * KPI 집계 스케줄러
 *
 * 매일 한국 시간 새벽 1시에 전날 KPI를 자동으로 집계합니다
 */
@Component
class KpiScheduler(
    private val kpiBatchService: KpiBatchService
) : Loggable {

    /**
     * 애플리케이션 시작 시 최근 7일치 KPI 집계 실행 (테스트용)
     */
    @PostConstruct
    fun aggregateRecentKpisOnStartup() {
        log.info { "========== 애플리케이션 시작 시 KPI 초기 집계 시작 ==========" }

        try {
            val today = DateTimeFormatter.getToday("ko")
            // 최근 7일치 KPI 집계
            for (i in 1..60) {
                val targetDate = today.minusDays(i.toLong())
                log.info { "집계 대상 날짜 (KST): $targetDate" }
                kpiBatchService.aggregateDailyKpi(targetDate)
            }

            log.info { "========== 애플리케이션 시작 시 KPI 초기 집계 완료 ==========" }
        } catch (e: Exception) {
            log.error(e) { "========== 애플리케이션 시작 시 KPI 초기 집계 실패 ==========" }
        }
    }

    /**
     * 매일 한국 시간 01:00에 전날 KPI 집계 실행
     * (UTC로 저장되어 있어도 한국 날짜 기준으로 집계)
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    fun runDailyKpiAggregation() {
        log.info { "========== KPI 자동 집계 시작 ==========" }

        try {
            // 한국 시간 기준 어제 날짜
            val yesterdayKst = DateTimeFormatter.getToday("ko").minusDays(1)

            log.info { "집계 대상 날짜 (KST): $yesterdayKst" }

            kpiBatchService.aggregateDailyKpi(yesterdayKst)

            log.info { "========== KPI 자동 집계 성공 ==========" }
        } catch (e: Exception) {
            log.error(e) { "========== KPI 자동 집계 실패 ==========" }
            // 예외를 던지지 않고 로그만 남김 (다음 스케줄 실행에 영향 없도록)
        }
    }
}
