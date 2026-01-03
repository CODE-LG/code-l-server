package codel.kpi.business

import codel.config.Loggable
import codel.kpi.infrastructure.DailyKpiJpaRepository
import codel.kpi.presentation.response.DailyKpiResponse
import codel.kpi.presentation.response.KpiSummaryResponse
import codel.question.domain.QuestionCategory
import codel.question.infrastructure.QuestionJpaRepository
import org.springframework.data.domain.PageRequest
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
    private val dailyKpiRepository: DailyKpiJpaRepository,
    private val questionRepository: QuestionJpaRepository,
    private val kpiChatRepository: codel.kpi.infrastructure.KpiChatRepository
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
            .findByTargetDateBetweenOrderByTargetDateAsc(startDate, endDate)

        if (dailyKpis.isEmpty()) {
            return KpiSummaryResponse(
                periodStart = startDate.toString(),
                periodEnd = endDate.toString(),
                signalSentSum = 0,
                signalAcceptedSum = 0,
                signalAcceptanceRateAvg = BigDecimal.ZERO,
                openChatroomsSum = 0,
                currentOpenChatroomsSum = 0,
                activeChatroomsSum = 0,
                chatActivityRateAvg = BigDecimal.ZERO,
                firstMessageRateAvg = BigDecimal.ZERO,
                threeTurnRateAvg = BigDecimal.ZERO,
                chatReturnRateAvg = BigDecimal.ZERO,
                avgMessageCountAvg = BigDecimal.ZERO,
                questionClickSum = 0,
                questionUsedChatroomsSum = 0,
                questionUsedAvgMessageCountAvg = BigDecimal.ZERO,
                questionNotUsedAvgMessageCountAvg = BigDecimal.ZERO,
                questionUsedThreeTurnRateAvg = BigDecimal.ZERO,
                questionNotUsedThreeTurnRateAvg = BigDecimal.ZERO,
                questionUsedChatReturnRateAvg = BigDecimal.ZERO,
                questionNotUsedChatReturnRateAvg = BigDecimal.ZERO,
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
        val currentOpenChatroomsSum = dailyKpis.sumOf { it.currentOpenChatroomsCount }
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
        val questionUsedAvgMessageCountAvg = calculateAverage(dailyKpis.map { it.questionUsedAvgMessageCount })
        val questionNotUsedAvgMessageCountAvg = calculateAverage(dailyKpis.map { it.questionNotUsedAvgMessageCount })
        val questionUsedThreeTurnRateAvg = calculateAverage(dailyKpis.map { it.questionUsedThreeTurnRate })
        val questionNotUsedThreeTurnRateAvg = calculateAverage(dailyKpis.map { it.questionNotUsedThreeTurnRate })
        val questionUsedChatReturnRateAvg = calculateAverage(dailyKpis.map { it.questionUsedChatReturnRate })
        val questionNotUsedChatReturnRateAvg = calculateAverage(dailyKpis.map { it.questionNotUsedChatReturnRate })
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
            currentOpenChatroomsSum = currentOpenChatroomsSum,
            activeChatroomsSum = activeChatroomsSum,
            chatActivityRateAvg = chatActivityRateAvg,
            firstMessageRateAvg = firstMessageRateAvg,
            threeTurnRateAvg = threeTurnRateAvg,
            chatReturnRateAvg = chatReturnRateAvg,
            avgMessageCountAvg = avgMessageCountAvg,

            // 질문
            questionClickSum = questionClickSum,
            questionUsedChatroomsSum = questionUsedChatroomsSum,
            questionUsedAvgMessageCountAvg = questionUsedAvgMessageCountAvg,
            questionNotUsedAvgMessageCountAvg = questionNotUsedAvgMessageCountAvg,
            questionUsedThreeTurnRateAvg = questionUsedThreeTurnRateAvg,
            questionNotUsedThreeTurnRateAvg = questionNotUsedThreeTurnRateAvg,
            questionUsedChatReturnRateAvg = questionUsedChatReturnRateAvg,
            questionNotUsedChatReturnRateAvg = questionNotUsedChatReturnRateAvg,

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
            .sortedBy { it.targetDate }
            .map { DailyKpiResponse.from(it) }
    }

    /**
     * 최근 N일간 KPI 조회
     */
    fun getRecentKpi(days: Int): List<DailyKpiResponse> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)
        return dailyKpiRepository
            .findByTargetDateBetweenOrderByTargetDateAsc(startDate, endDate)
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

    /**
     * 질문 콘텐츠 인사이트 조회 (프로필 대표 질문 통계)
     */
    fun getQuestionInsights(startDate: LocalDate? = null, endDate: LocalDate? = null): Map<String, Any> {
        // TOP 10 인기 질문 - 날짜 범위가 지정된 경우 해당 기간 데이터 사용
        val topQuestions = if (startDate != null && endDate != null) {
            val utcStart = codel.common.util.DateTimeFormatter.convertKstToUtc(startDate.atStartOfDay())
            val utcEnd = codel.common.util.DateTimeFormatter.convertKstToUtc(endDate.atTime(java.time.LocalTime.MAX))
            questionRepository.findTopSelectedQuestionsByDateRange(utcStart, utcEnd, PageRequest.of(0, 10))
        } else {
            questionRepository.findTopSelectedQuestions(PageRequest.of(0, 10))
        }.map { row ->
            mapOf(
                "questionId" to row[0],
                "content" to row[1],
                "category" to row[2],
                "selectionCount" to row[3]
            )
        }

        // 카테고리별 통계 - 전체 활성 질문 사용
        val categoryStats = questionRepository.findQuestionCategoryStats()
            .map { row ->
                mapOf(
                    "category" to (row[0] as QuestionCategory).name,
                    "count" to row[1]
                )
            }

        return mapOf(
            "topQuestions" to topQuestions,
            "categoryStats" to categoryStats
        )
    }

    /**
     * 실시간 채팅방 통계 조회
     */
    fun getChatroomStatistics(): Map<String, Any> {
        val now = java.time.LocalDateTime.now()

        // 전체 채팅방 수
        val totalChatrooms = kpiChatRepository.count().toInt()

        // 열린 채팅방 수 (DISABLED가 아닌 것)
        val openChatrooms = kpiChatRepository.countOpenChatroomsAsOfDate(now)

        // 활성 채팅방 수 (최근 7일 내 활동)
        val sevenDaysAgo = now.minusDays(7)
        val activeChatrooms = kpiChatRepository.countActiveChatroomsAsOfDate(now, sevenDaysAgo)

        // 활성 채팅방 비율 (활성 채팅방 / 열린 채팅방)
        val activeChatroomRate = if (openChatrooms > 0) {
            (activeChatrooms.toBigDecimal() / openChatrooms.toBigDecimal())
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        return mapOf(
            "totalChatrooms" to totalChatrooms,
            "openChatrooms" to openChatrooms,
            "activeChatrooms" to activeChatrooms,
            "activeChatroomRate" to activeChatroomRate
        )
    }
}
