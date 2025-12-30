package codel.kpi.presentation

import codel.kpi.business.KpiService
import codel.kpi.presentation.response.DailyKpiResponse
import codel.kpi.presentation.response.KpiSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/v1/admin/kpi")
@Tag(name = "KPI Dashboard", description = "KPI 대시보드 API")
class KpiController(
    private val kpiService: KpiService
) {

    @GetMapping("/daily/{date}")
    @Operation(summary = "특정 날짜 KPI 조회", description = "특정 날짜의 KPI 데이터를 조회합니다")
    fun getDailyKpi(
        @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate
    ): ResponseEntity<DailyKpiResponse> {
        val kpi = kpiService.getDailyKpi(date)
        return if (kpi != null) {
            ResponseEntity.ok(kpi)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/summary")
    @Operation(summary = "기간별 KPI 요약 조회", description = "시작일부터 종료일까지의 KPI 요약 데이터를 조회합니다")
    fun getKpiSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<KpiSummaryResponse> {
        val summary = kpiService.getKpiSummary(startDate, endDate)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/all")
    @Operation(summary = "전체 KPI 목록 조회", description = "모든 날짜의 KPI 데이터 목록을 조회합니다")
    fun getAllDailyKpis(): ResponseEntity<List<DailyKpiResponse>> {
        val kpis = kpiService.getAllDailyKpis()
        return ResponseEntity.ok(kpis)
    }
}
