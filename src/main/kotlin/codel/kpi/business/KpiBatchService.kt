package codel.kpi.business

import codel.chat.domain.Chat
import codel.chat.domain.ChatContentType
import codel.chat.domain.ChatRoom
import codel.chat.domain.ChatSenderType
import codel.common.util.DateTimeFormatter
import codel.config.Loggable
import codel.kpi.domain.DailyKpi
import codel.kpi.infrastructure.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * KPI 배치 집계 서비스
 *
 * 한국 시간 기준 일별 KPI 집계
 */
@Service
@Transactional
class KpiBatchService(
    private val dailyKpiRepository: DailyKpiJpaRepository,
    private val kpiSignalRepository: KpiSignalRepository,
    private val kpiChatRepository: KpiChatRepository,
    private val kpiChatMessageRepository: KpiChatMessageRepository,
    private val kpiQuestionRepository: KpiQuestionRepository,
    private val kpiCodeUnlockRepository: KpiCodeUnlockRepository
) : Loggable {

    /**
     * 한국 시간 기준 특정 날짜의 KPI를 집계합니다
     *
     * @param kstDate 한국 시간 기준 날짜 (예: 2025-01-01)
     */
    fun aggregateDailyKpi(kstDate: LocalDate) {
        log.info { "===== KPI 집계 시작 (KST 기준): $kstDate =====" }

        // 한국 날짜를 UTC 시간 범위로 변환
        val (utcStart, utcEnd) = DateTimeFormatter.getUtcRangeForKstDate(kstDate)
        log.info { "UTC 변환: $kstDate (KST) -> $utcStart ~ $utcEnd (UTC)" }

        // 이미 집계된 데이터가 있으면 갱신, 없으면 생성
        val existingKpi = dailyKpiRepository.findByTargetDate(kstDate)
        val dailyKpi = existingKpi ?: DailyKpi(targetDate = kstDate)

        // 각 KPI 집계
        aggregateSignalKpi(dailyKpi, utcStart, utcEnd)
        aggregateChatKpi(dailyKpi, kstDate, utcStart, utcEnd)
        aggregateQuestionKpi(dailyKpi, utcStart, utcEnd)
        aggregateCodeUnlockKpi(dailyKpi, utcStart, utcEnd)
        aggregateClosedChatKpi(dailyKpi, utcStart, utcEnd)

        // 저장
        dailyKpiRepository.save(dailyKpi)

        log.info { "===== KPI 집계 완료 (KST 기준): $kstDate =====" }
        log.info {
            "시그널: 보낸=${dailyKpi.signalSentCount}, 수락=${dailyKpi.signalAcceptedCount} | " +
            "채팅: 열림=${dailyKpi.openChatroomsCount}, 활성=${dailyKpi.activeChatroomsCount} | " +
            "질문: ${dailyKpi.questionUsedChatroomsCount}개 채팅방 사용"
        }
    }

    /**
     * 1. 시그널 KPI 집계
     */
    private fun aggregateSignalKpi(
        dailyKpi: DailyKpi,
        utcStart: LocalDateTime,
        utcEnd: LocalDateTime
    ) {
        // 특정 날짜에 보낸 시그널 수
        dailyKpi.signalSentCount = kpiSignalRepository.countByCreatedAtBetween(utcStart, utcEnd)

        // 특정 날짜에 보낸 시그널 중 현재까지 승인된 개수
        dailyKpi.signalAcceptedCount = kpiSignalRepository.countApprovedByCreatedAtBetween(utcStart, utcEnd)

        log.debug {
            "시그널 KPI: 보낸 수=${dailyKpi.signalSentCount}, " +
            "수락 수=${dailyKpi.signalAcceptedCount}, " +
            "수락률=${dailyKpi.getSignalAcceptanceRate()}%"
        }
    }

    /**
     * 2. 채팅 KPI 집계 (가장 복잡)
     */
    private fun aggregateChatKpi(
        dailyKpi: DailyKpi,
        kstDate: LocalDate,
        utcStart: LocalDateTime,
        utcEnd: LocalDateTime
    ) {
        // 해당 날짜에 생성된 채팅방 수
        dailyKpi.openChatroomsCount = kpiChatRepository.countByCreatedAtBetween(utcStart, utcEnd)

        // 한국 날짜 끝 시점을 UTC로 변환 (해당 날짜 23:59:59 KST)
        val kstEndOfDay = kstDate.atTime(LocalTime.MAX)
        val utcAsOfDate = DateTimeFormatter.convertKstToUtc(kstEndOfDay)

        // 현재 열려있는 채팅방 수 (endDate 시점 기준)
        dailyKpi.currentOpenChatroomsCount = kpiChatRepository.countOpenChatroomsAsOfDate(utcAsOfDate)

        // 7일 전 시점 (한국 기준)
        val kstSevenDaysAgo = kstDate.minusDays(7).atStartOfDay()
        val utcSevenDaysAgo = DateTimeFormatter.convertKstToUtc(kstSevenDaysAgo)

        // 활성 채팅방 수 (endDate 기준 최근 7일 내 updated_at, 중복 카운트 방지)
        dailyKpi.activeChatroomsCount = kpiChatRepository.countActiveChatroomsAsOfDate(
            utcAsOfDate,
            utcSevenDaysAgo
        )

        // 해당 날짜에 생성된 채팅방 조회
        val createdChatRooms = kpiChatRepository.findByCreatedAtBetween(utcStart, utcEnd)

        if (createdChatRooms.isEmpty()) {
            log.debug { "채팅 KPI: 생성된 채팅방 없음" }
            return
        }

        // FMR, 3턴 비율, CRR, 평균 메시지 수 계산
        var firstMessageCount = 0
        var threeTurnCount = 0
        var returnWithin24hCount = 0
        var totalMessageCount = 0L

        createdChatRooms.forEach { chatRoom ->
            val messages = kpiChatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)

            // 템플릿 메시지 6개 이후 실제 메시지가 있는지 확인
            if (messages.size > 6) {
                firstMessageCount++
                totalMessageCount += messages.size

                if (hasThreeTurnOrMore(messages)) {
                    threeTurnCount++
                }

                if (hasReturnWithin24Hours(messages)) {
                    returnWithin24hCount++
                }
            }
        }

        // 비율 계산
        val totalChatRooms = createdChatRooms.size
        dailyKpi.firstMessageRate = calculateRate(firstMessageCount, totalChatRooms)
        dailyKpi.threeTurnRate = calculateRate(threeTurnCount, totalChatRooms)
        dailyKpi.chatReturnRate = calculateRate(returnWithin24hCount, totalChatRooms)
        dailyKpi.avgMessageCount = if (totalChatRooms > 0) {
            BigDecimal(totalMessageCount).divide(BigDecimal(totalChatRooms), 2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO

        log.debug {
            "채팅 KPI: 열린=${dailyKpi.openChatroomsCount}, " +
            "활성=${dailyKpi.activeChatroomsCount}, " +
            "FMR=${dailyKpi.firstMessageRate}%, " +
            "3턴=${dailyKpi.threeTurnRate}%, " +
            "CRR=${dailyKpi.chatReturnRate}%, " +
            "평균메시지=${dailyKpi.avgMessageCount}"
        }
    }

    /**
     * 3턴 이상 대화 확인
     * (템플릿 6개 제외, 두 멤버가 각각 3개 이상 메시지)
     */
    private fun hasThreeTurnOrMore(messages: List<Chat>): Boolean {
        // 템플릿 이후의 실제 메시지만 필터링
        val realMessages = messages
            .drop(6)  // 템플릿 6개 제외
            .filter { it.senderType == ChatSenderType.USER && it.chatContentType == ChatContentType.TEXT }

        if (realMessages.size < 6) return false

        // 두 멤버가 각각 최소 3개씩 메시지를 보냈는지 확인
        val messagesByMember = realMessages.groupBy { it.fromChatRoomMember?.member?.id }

        return messagesByMember.size >= 2 &&  // 두 명이 대화
               messagesByMember.all { it.value.size >= 3 }  // 각자 최소 3개 메시지
    }

    /**
     * 24시간 내 재방문 확인
     * (템플릿 이후 첫 메시지 후 24시간 내 추가 메시지)
     */
    private fun hasReturnWithin24Hours(messages: List<Chat>): Boolean {
        // 템플릿 이후의 실제 메시지만 필터링
        val realMessages = messages
            .drop(6)
            .filter { it.senderType == ChatSenderType.USER && it.chatContentType == ChatContentType.TEXT }

        if (realMessages.size < 2) return false

        val firstMessageTime = realMessages.first().getSentAtOrThrow()
        val secondMessageTime = realMessages[1].getSentAtOrThrow()

        val hoursDiff = Duration.between(firstMessageTime, secondMessageTime).toHours()

        return hoursDiff <= 24
    }

    /**
     * 3. 질문 KPI 집계
     */
    private fun aggregateQuestionKpi(
        dailyKpi: DailyKpi,
        utcStart: LocalDateTime,
        utcEnd: LocalDateTime
    ) {
        // 초기 질문 제외, 질문하기 버튼 클릭으로 생성된 질문만 집계
        dailyKpi.questionUsedChatroomsCount = kpiQuestionRepository
            .countDistinctChatRoomsByCreatedAtBetweenExcludingInitial(utcStart, utcEnd)

        // 질문 클릭 수 (추후 이벤트 로그 연동 가능)
        dailyKpi.questionClickCount = kpiQuestionRepository
            .countQuestionClicksByCreatedAtBetweenExcludingInitial(utcStart, utcEnd)

        // 해당 날짜에 생성된 채팅방 조회하여 질문 사용 여부별 성과 비교
        val createdChatRooms = kpiChatRepository.findByCreatedAtBetween(utcStart, utcEnd)

        if (createdChatRooms.isEmpty()) {
            log.debug { "질문 KPI: 생성된 채팅방 없음 - 비교 데이터 없음" }
            return
        }

        // 해당 날짜에 생성된 채팅방 ID 목록
        val createdChatRoomIds = createdChatRooms.mapNotNull { it.id }

        // 이 채팅방들 중에서 (언제든) 질문을 사용한 채팅방 ID 조회
        val questionUsedChatRoomIds = if (createdChatRoomIds.isNotEmpty()) {
            kpiQuestionRepository
                .findChatRoomIdsWithQuestionsFromList(createdChatRoomIds)
                .toSet()
        } else {
            emptySet<Long>()
        }

        // 질문 사용 채팅방과 미사용 채팅방 분리
        val (questionUsedRooms, questionNotUsedRooms) = createdChatRooms.partition {
            it.id in questionUsedChatRoomIds
        }

        // 질문 사용 채팅방 메트릭 계산
        val usedMetrics = calculateChatMetrics(questionUsedRooms)
        dailyKpi.questionUsedAvgMessageCount = usedMetrics.avgMessageCount
        dailyKpi.questionUsedThreeTurnRate = usedMetrics.threeTurnRate
        dailyKpi.questionUsedChatReturnRate = usedMetrics.chatReturnRate

        // 질문 미사용 채팅방 메트릭 계산
        val notUsedMetrics = calculateChatMetrics(questionNotUsedRooms)
        dailyKpi.questionNotUsedAvgMessageCount = notUsedMetrics.avgMessageCount
        dailyKpi.questionNotUsedThreeTurnRate = notUsedMetrics.threeTurnRate
        dailyKpi.questionNotUsedChatReturnRate = notUsedMetrics.chatReturnRate

        log.debug {
            "질문 KPI: 사용 채팅방=${dailyKpi.questionUsedChatroomsCount}, " +
            "클릭 수=${dailyKpi.questionClickCount} | " +
            "비교 메트릭 - 전체=${createdChatRooms.size}, " +
            "질문 사용=${questionUsedRooms.size}, 미사용=${questionNotUsedRooms.size} | " +
            "사용 평균메시지=${dailyKpi.questionUsedAvgMessageCount}, " +
            "미사용 평균메시지=${dailyKpi.questionNotUsedAvgMessageCount}"
        }
    }

    /**
     * 채팅방 그룹의 메트릭 계산 (평균 메시지, 3턴 비율, CRR)
     */
    private fun calculateChatMetrics(chatRooms: List<ChatRoom>): ChatMetrics {
        if (chatRooms.isEmpty()) {
            return ChatMetrics(
                avgMessageCount = BigDecimal.ZERO,
                threeTurnRate = BigDecimal.ZERO,
                chatReturnRate = BigDecimal.ZERO
            )
        }

        var threeTurnCount = 0
        var returnWithin24hCount = 0
        var totalMessageCount = 0L

        chatRooms.forEach { chatRoom ->
            val messages = kpiChatMessageRepository.findByChatRoomOrderBySentAtAsc(chatRoom)

            // 템플릿 메시지 6개 이후 실제 메시지가 있는지 확인
            if (messages.size > 6) {
                totalMessageCount += messages.size

                if (hasThreeTurnOrMore(messages)) {
                    threeTurnCount++
                }

                if (hasReturnWithin24Hours(messages)) {
                    returnWithin24hCount++
                }
            }
        }

        val totalChatRooms = chatRooms.size
        return ChatMetrics(
            avgMessageCount = if (totalChatRooms > 0) {
                BigDecimal(totalMessageCount).divide(BigDecimal(totalChatRooms), 2, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO,
            threeTurnRate = calculateRate(threeTurnCount, totalChatRooms),
            chatReturnRate = calculateRate(returnWithin24hCount, totalChatRooms)
        )
    }

    /**
     * 채팅 메트릭 데이터 클래스
     */
    private data class ChatMetrics(
        val avgMessageCount: BigDecimal,
        val threeTurnRate: BigDecimal,
        val chatReturnRate: BigDecimal
    )

    /**
     * 4. 코드해제 KPI 집계
     */
    private fun aggregateCodeUnlockKpi(
        dailyKpi: DailyKpi,
        utcStart: LocalDateTime,
        utcEnd: LocalDateTime
    ) {
        dailyKpi.codeUnlockRequestCount = kpiCodeUnlockRepository
            .countByCreatedAtBetween(utcStart, utcEnd)

        dailyKpi.codeUnlockApprovedCount = kpiCodeUnlockRepository
            .countApprovedByUpdatedAtBetween(utcStart, utcEnd)

        log.debug {
            "코드해제 KPI: 요청=${dailyKpi.codeUnlockRequestCount}, " +
            "승인=${dailyKpi.codeUnlockApprovedCount}, " +
            "승인율=${dailyKpi.getCodeUnlockApprovalRate()}%"
        }
    }

    /**
     * 5. 종료된 채팅방 KPI 집계
     */
    private fun aggregateClosedChatKpi(
        dailyKpi: DailyKpi,
        utcStart: LocalDateTime,
        utcEnd: LocalDateTime
    ) {
        val closedChatrooms = kpiChatRepository.findClosedByUpdatedAtBetween(utcStart, utcEnd)
        dailyKpi.closedChatroomsCount = closedChatrooms.size

        if (closedChatrooms.isNotEmpty()) {
            val totalDurationDays = closedChatrooms.sumOf { chatRoom ->
                Duration.between(chatRoom.createdAt, chatRoom.updatedAt).toDays()
            }
            dailyKpi.avgChatDurationDays = BigDecimal(totalDurationDays)
                .divide(BigDecimal(closedChatrooms.size), 2, RoundingMode.HALF_UP)
        }

        log.debug {
            "종료 채팅 KPI: 종료 수=${dailyKpi.closedChatroomsCount}, " +
            "평균 유지 기간=${dailyKpi.avgChatDurationDays}일"
        }
    }

    /**
     * 비율 계산 헬퍼 (백분율)
     */
    private fun calculateRate(numerator: Int, denominator: Int): BigDecimal {
        return if (denominator > 0) {
            (BigDecimal(numerator).divide(BigDecimal(denominator), 4, RoundingMode.HALF_UP))
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
}
