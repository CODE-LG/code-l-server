package codel.kpi.presentation

import codel.kpi.business.KpiBatchService
import codel.kpi.business.KpiService
import codel.kpi.presentation.response.DailyKpiResponse
import codel.kpi.presentation.response.KpiSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@Controller
@RequestMapping("/v1/admin/kpi")
@Tag(name = "KPI Dashboard", description = "KPI 대시보드 API")
class KpiController(
    private val kpiService: KpiService,
    private val kpiBatchService: KpiBatchService
) {

    @GetMapping
    fun kpiDashboard(model: Model): String {
        return "kpi-dashboard"
    }

    @GetMapping("/daily/{date}")
    @ResponseBody
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
    @ResponseBody
    @Operation(summary = "기간별 KPI 요약 조회", description = "시작일부터 종료일까지의 KPI 요약 데이터를 조회합니다")
    fun getKpiSummary(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate
    ): ResponseEntity<KpiSummaryResponse> {
        val summary = kpiService.getKpiSummary(startDate, endDate)
        return ResponseEntity.ok(summary)
    }

    @GetMapping("/all")
    @ResponseBody
    @Operation(summary = "전체 KPI 목록 조회", description = "모든 날짜의 KPI 데이터 목록을 조회합니다")
    fun getAllDailyKpis(): ResponseEntity<List<DailyKpiResponse>> {
        val kpis = kpiService.getAllDailyKpis()
        return ResponseEntity.ok(kpis)
    }

    @PostMapping("/aggregate")
    @ResponseBody
    @Operation(summary = "KPI 수동 집계", description = "특정 날짜의 KPI를 수동으로 집계합니다. 날짜를 지정하지 않으면 어제 날짜로 집계합니다.")
    fun aggregateKpi(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?
    ): ResponseEntity<Map<String, Any>> {
        val targetDate = date ?: LocalDate.now().minusDays(1)

        return try {
            kpiBatchService.aggregateDailyKpi(targetDate)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "KPI 집계가 완료되었습니다.",
                "date" to targetDate.toString()
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "KPI 집계 중 오류가 발생했습니다: ${e.message}",
                "date" to targetDate.toString()
            ))
        }
    }
}
