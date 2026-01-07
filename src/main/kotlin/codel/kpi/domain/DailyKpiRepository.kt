package codel.kpi.domain

import java.time.LocalDate

interface DailyKpiRepository {
    fun save(dailyKpi: DailyKpi): DailyKpi
    fun findByTargetDate(targetDate: LocalDate): DailyKpi?
    fun findByTargetDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyKpi>
    fun findAll(): List<DailyKpi>
}
