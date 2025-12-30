package codel.kpi.presentation.response

import java.math.BigDecimal

/**
 * 기간별 KPI 요약 응답
 */
data class KpiSummaryResponse(
    val periodStart: String,
    val periodEnd: String,

    // 시그널 KPI (합계)
    val signalSentSum: Int,
    val signalAcceptedSum: Int,
    val signalAcceptanceRateAvg: BigDecimal,

    // 채팅 KPI (합계 및 평균)
    val openChatroomsSum: Int,
    val activeChatroomsSum: Int,
    val chatActivityRateAvg: BigDecimal,
    val firstMessageRateAvg: BigDecimal,
    val threeTurnRateAvg: BigDecimal,
    val chatReturnRateAvg: BigDecimal,
    val avgMessageCountAvg: BigDecimal,

    // 질문 KPI (합계 및 평균)
    val questionClickSum: Int,
    val questionUsedChatroomsSum: Int,
    val questionUsedAvgMessageCountAvg: BigDecimal,
    val questionNotUsedAvgMessageCountAvg: BigDecimal,
    val questionUsedThreeTurnRateAvg: BigDecimal,
    val questionNotUsedThreeTurnRateAvg: BigDecimal,
    val questionUsedChatReturnRateAvg: BigDecimal,
    val questionNotUsedChatReturnRateAvg: BigDecimal,

    // 코드해제 KPI (합계)
    val codeUnlockRequestSum: Int,
    val codeUnlockApprovedSum: Int,
    val codeUnlockApprovalRateAvg: BigDecimal,

    // 종료 KPI (합계 및 평균)
    val closedChatroomsSum: Int,
    val avgChatDurationDaysAvg: BigDecimal,

    // 일별 상세 데이터
    val dailyKpis: List<DailyKpiResponse>
)
