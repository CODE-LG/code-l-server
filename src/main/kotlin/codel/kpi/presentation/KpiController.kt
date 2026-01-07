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

    @PostMapping("/aggregate-range")
    @ResponseBody
    @Operation(
        summary = "KPI 대량 수동 집계",
        description = "시작일부터 종료일까지의 KPI를 한 번에 집계합니다. 날짜를 지정하지 않으면 최근 30일을 집계합니다."
    )
    fun aggregateKpiRange(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<Map<String, Any>> {
        val end = endDate ?: LocalDate.now().minusDays(1)
        val start = startDate ?: end.minusDays(29)

        // 날짜 범위 검증
        if (start.isAfter(end)) {
            return ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to "시작일이 종료일보다 늦을 수 없습니다.",
                "startDate" to start.toString(),
                "endDate" to end.toString()
            ))
        }

        val results = mutableListOf<Map<String, Any>>()
        var successCount = 0
        var failCount = 0

        // 시작일부터 종료일까지 순회하며 집계
        var currentDate = start
        while (!currentDate.isAfter(end)) {
            try {
                kpiBatchService.aggregateDailyKpi(currentDate)
                results.add(mapOf(
                    "date" to currentDate.toString(),
                    "success" to true
                ))
                successCount++
            } catch (e: Exception) {
                results.add(mapOf(
                    "date" to currentDate.toString(),
                    "success" to false,
                    "error" to (e.message ?: "Unknown error")
                ))
                failCount++
            }
            currentDate = currentDate.plusDays(1)
        }

        return ResponseEntity.ok(mapOf(
            "success" to true,
            "message" to "KPI 대량 집계가 완료되었습니다.",
            "startDate" to start.toString(),
            "endDate" to end.toString(),
            "totalDays" to results.size,
            "successCount" to successCount,
            "failCount" to failCount,
            "details" to results
        ))
    }

    @GetMapping("/question-insights")
    @ResponseBody
    @Operation(summary = "질문 콘텐츠 인사이트 조회", description = "프로필 대표 질문 인기도 및 카테고리 통계를 조회합니다")
    fun getQuestionInsights(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?
    ): ResponseEntity<Map<String, Any>> {
        val insights = kpiService.getQuestionInsights(startDate, endDate)
        return ResponseEntity.ok(insights)
    }

    @GetMapping("/chatroom-statistics")
    @ResponseBody
    @Operation(summary = "실시간 채팅방 통계 조회", description = "전체 채팅방, 열린 채팅방, 활성 채팅방 수 및 활성률을 조회합니다")
    fun getChatroomStatistics(): ResponseEntity<Map<String, Any>> {
        val statistics = kpiService.getChatroomStatistics()
        return ResponseEntity.ok(statistics)
    }
}
