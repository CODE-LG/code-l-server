package codel.kpi.business

import codel.config.Loggable
import codel.kpi.infrastructure.DailyKpiJpaRepository
import codel.kpi.presentation.response.DailyKpiResponse
import codel.kpi.presentation.response.KpiSummaryResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

/**
 * KPI 조회 서비스
 */
@Service
@Transactional(readOnly = true)
class KpiService(
    private val dailyKpiRepository: DailyKpiJpaRepository
) : Loggable {

    /**
     * 특정 날짜의 KPI 조회
     */
    fun getDailyKpi(date: LocalDate): DailyKpiResponse? {
        return dailyKpiRepository.findByTargetDate(date)?.let {
            DailyKpiResponse.from(it)
        }
    }

    /**
     * 기간별 KPI 요약 조회
     */
    fun getKpiSummary(startDate: LocalDate, endDate: LocalDate): KpiSummaryResponse {
        val dailyKpis = dailyKpiRepository
            .findByTargetDateBetweenOrderByTargetDateDesc(startDate, endDate)

        if (dailyKpis.isEmpty()) {
            return KpiSummaryResponse(
                periodStart = startDate.toString(),
                periodEnd = endDate.toString(),
                signalSentSum = 0,
                signalAcceptedSum = 0,
                signalAcceptanceRateAvg = BigDecimal.ZERO,
                openChatroomsSum = 0,
                activeChatroomsSum = 0,
                chatActivityRateAvg = BigDecimal.ZERO,
                firstMessageRateAvg = BigDecimal.ZERO,
                threeTurnRateAvg = BigDecimal.ZERO,
                chatReturnRateAvg = BigDecimal.ZERO,
                avgMessageCountAvg = BigDecimal.ZERO,
                questionClickSum = 0,
                questionUsedChatroomsSum = 0,
                codeUnlockRequestSum = 0,
                codeUnlockApprovedSum = 0,
                codeUnlockApprovalRateAvg = BigDecimal.ZERO,
                closedChatroomsSum = 0,
                avgChatDurationDaysAvg = BigDecimal.ZERO,
                dailyKpis = emptyList()
            )
        }

        val count = dailyKpis.size

        // 합계 계산
        val signalSentSum = dailyKpis.sumOf { it.signalSentCount }
        val signalAcceptedSum = dailyKpis.sumOf { it.signalAcceptedCount }
        val openChatroomsSum = dailyKpis.sumOf { it.openChatroomsCount }
        val activeChatroomsSum = dailyKpis.sumOf { it.activeChatroomsCount }
        val questionClickSum = dailyKpis.sumOf { it.questionClickCount }
        val questionUsedChatroomsSum = dailyKpis.sumOf { it.questionUsedChatroomsCount }
        val codeUnlockRequestSum = dailyKpis.sumOf { it.codeUnlockRequestCount }
        val codeUnlockApprovedSum = dailyKpis.sumOf { it.codeUnlockApprovedCount }
        val closedChatroomsSum = dailyKpis.sumOf { it.closedChatroomsCount }

        // 평균 계산
        val signalAcceptanceRateAvg = calculateAverage(dailyKpis.map { it.getSignalAcceptanceRate() })
        val chatActivityRateAvg = calculateAverage(dailyKpis.map { it.getChatActivityRate() })
        val firstMessageRateAvg = calculateAverage(dailyKpis.map { it.firstMessageRate })
        val threeTurnRateAvg = calculateAverage(dailyKpis.map { it.threeTurnRate })
        val chatReturnRateAvg = calculateAverage(dailyKpis.map { it.chatReturnRate })
        val avgMessageCountAvg = calculateAverage(dailyKpis.map { it.avgMessageCount })
        val codeUnlockApprovalRateAvg = calculateAverage(dailyKpis.map { it.getCodeUnlockApprovalRate() })
        val avgChatDurationDaysAvg = calculateAverage(dailyKpis.map { it.avgChatDurationDays })

        return KpiSummaryResponse(
            periodStart = startDate.toString(),
            periodEnd = endDate.toString(),

            // 시그널
            signalSentSum = signalSentSum,
            signalAcceptedSum = signalAcceptedSum,
            signalAcceptanceRateAvg = signalAcceptanceRateAvg,

            // 채팅
            openChatroomsSum = openChatroomsSum,
            activeChatroomsSum = activeChatroomsSum,
            chatActivityRateAvg = chatActivityRateAvg,
            firstMessageRateAvg = firstMessageRateAvg,
            threeTurnRateAvg = threeTurnRateAvg,
            chatReturnRateAvg = chatReturnRateAvg,
            avgMessageCountAvg = avgMessageCountAvg,

            // 질문
            questionClickSum = questionClickSum,
            questionUsedChatroomsSum = questionUsedChatroomsSum,

            // 코드해제
            codeUnlockRequestSum = codeUnlockRequestSum,
            codeUnlockApprovedSum = codeUnlockApprovedSum,
            codeUnlockApprovalRateAvg = codeUnlockApprovalRateAvg,

            // 종료
            closedChatroomsSum = closedChatroomsSum,
            avgChatDurationDaysAvg = avgChatDurationDaysAvg,

            // 일별 데이터
            dailyKpis = dailyKpis.map { DailyKpiResponse.from(it) }
        )
    }

    /**
     * 전체 KPI 목록 조회
     */
    fun getAllDailyKpis(): List<DailyKpiResponse> {
        return dailyKpiRepository.findAll()
            .sortedByDescending { it.targetDate }
            .map { DailyKpiResponse.from(it) }
    }

    /**
     * BigDecimal 리스트의 평균 계산
     */
    private fun calculateAverage(values: List<BigDecimal>): BigDecimal {
        if (values.isEmpty()) return BigDecimal.ZERO

        val sum = values.fold(BigDecimal.ZERO) { acc, value -> acc.add(value) }
        return sum.divide(BigDecimal(values.size), 2, RoundingMode.HALF_UP)
    }
}
