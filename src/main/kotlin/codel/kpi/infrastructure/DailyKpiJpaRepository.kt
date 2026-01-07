package codel.kpi.infrastructure

import codel.kpi.domain.DailyKpi
import codel.kpi.domain.DailyKpiRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface DailyKpiJpaRepository : JpaRepository<DailyKpi, Long>, DailyKpiRepository {
    override fun findByTargetDate(targetDate: LocalDate): DailyKpi?
    fun findByTargetDateBetweenOrderByTargetDateAsc(startDate: LocalDate, endDate: LocalDate): List<DailyKpi>
    override fun findByTargetDateBetween(startDate: LocalDate, endDate: LocalDate): List<DailyKpi>
}
