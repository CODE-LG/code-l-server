package codel.kpi.presentation.response

import codel.kpi.domain.DailyKpi
import java.math.BigDecimal
import java.time.LocalDate

data class DailyKpiResponse(
    val targetDate: LocalDate,

    // 1. 시그널 KPI
    val signalSentCount: Int,
    val signalAcceptedCount: Int,
    val signalAcceptanceRate: BigDecimal,

    // 2. 채팅 KPI
    val openChatroomsCount: Int,
    val currentOpenChatroomsCount: Int,
    val activeChatroomsCount: Int,
    val chatActivityRate: BigDecimal,
    val firstMessageRate: BigDecimal,
    val threeTurnRate: BigDecimal,
    val chatReturnRate: BigDecimal,
    val avgMessageCount: BigDecimal,

    // 3. 질문추천 KPI
    val questionClickCount: Int,
    val questionUsedChatroomsCount: Int,
    val questionUsedAvgMessageCount: BigDecimal,
    val questionNotUsedAvgMessageCount: BigDecimal,
    val questionUsedThreeTurnRate: BigDecimal,
    val questionNotUsedThreeTurnRate: BigDecimal,
    val questionUsedChatReturnRate: BigDecimal,
    val questionNotUsedChatReturnRate: BigDecimal,

    // 4. 코드해제 KPI
    val codeUnlockRequestCount: Int,
    val codeUnlockApprovedCount: Int,
    val codeUnlockApprovalRate: BigDecimal,

    // 5. 종료된 채팅방 KPI
    val closedChatroomsCount: Int,
    val avgChatDurationDays: BigDecimal
) {
    companion object {
        fun from(dailyKpi: DailyKpi): DailyKpiResponse {
            return DailyKpiResponse(
                targetDate = dailyKpi.targetDate,

                // 시그널
                signalSentCount = dailyKpi.signalSentCount,
                signalAcceptedCount = dailyKpi.signalAcceptedCount,
                signalAcceptanceRate = dailyKpi.getSignalAcceptanceRate(),

                // 채팅
                openChatroomsCount = dailyKpi.openChatroomsCount,
                currentOpenChatroomsCount = dailyKpi.currentOpenChatroomsCount,
                activeChatroomsCount = dailyKpi.activeChatroomsCount,
                chatActivityRate = dailyKpi.getChatActivityRate(),
                firstMessageRate = dailyKpi.firstMessageRate,
                threeTurnRate = dailyKpi.threeTurnRate,
                chatReturnRate = dailyKpi.chatReturnRate,
                avgMessageCount = dailyKpi.avgMessageCount,

                // 질문
                questionClickCount = dailyKpi.questionClickCount,
                questionUsedChatroomsCount = dailyKpi.questionUsedChatroomsCount,
                questionUsedAvgMessageCount = dailyKpi.questionUsedAvgMessageCount,
                questionNotUsedAvgMessageCount = dailyKpi.questionNotUsedAvgMessageCount,
                questionUsedThreeTurnRate = dailyKpi.questionUsedThreeTurnRate,
                questionNotUsedThreeTurnRate = dailyKpi.questionNotUsedThreeTurnRate,
                questionUsedChatReturnRate = dailyKpi.questionUsedChatReturnRate,
                questionNotUsedChatReturnRate = dailyKpi.questionNotUsedChatReturnRate,

                // 코드해제
                codeUnlockRequestCount = dailyKpi.codeUnlockRequestCount,
                codeUnlockApprovedCount = dailyKpi.codeUnlockApprovedCount,
                codeUnlockApprovalRate = dailyKpi.getCodeUnlockApprovalRate(),

                // 종료
                closedChatroomsCount = dailyKpi.closedChatroomsCount,
                avgChatDurationDays = dailyKpi.avgChatDurationDays
            )
        }
    }
}
