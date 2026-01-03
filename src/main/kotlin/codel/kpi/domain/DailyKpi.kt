package codel.kpi.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Entity
@Table(name = "daily_kpi")
class DailyKpi(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    /**
     * 한국 시간 기준 집계 날짜
     * (DB 저장 시간은 UTC이지만, 이 날짜는 KST 기준)
     */
    @Column(nullable = false, unique = true)
    val targetDate: LocalDate,

    // 1. 시그널 KPI
    var signalSentCount: Int = 0,
    var signalAcceptedCount: Int = 0,

    // 2. 채팅 KPI
    var openChatroomsCount: Int = 0,
    var currentOpenChatroomsCount: Int = 0,
    var activeChatroomsCount: Int = 0,
    var firstMessageRate: BigDecimal = BigDecimal.ZERO,
    var threeTurnRate: BigDecimal = BigDecimal.ZERO,
    var chatReturnRate: BigDecimal = BigDecimal.ZERO,
    var avgMessageCount: BigDecimal = BigDecimal.ZERO,

    // 3. 질문추천 KPI
    var questionClickCount: Int = 0,
    var questionUsedChatroomsCount: Int = 0,
    var questionUsedAvgMessageCount: BigDecimal = BigDecimal.ZERO,
    var questionNotUsedAvgMessageCount: BigDecimal = BigDecimal.ZERO,
    var questionUsedThreeTurnRate: BigDecimal = BigDecimal.ZERO,
    var questionNotUsedThreeTurnRate: BigDecimal = BigDecimal.ZERO,
    var questionUsedChatReturnRate: BigDecimal = BigDecimal.ZERO,
    var questionNotUsedChatReturnRate: BigDecimal = BigDecimal.ZERO,

    // 4. 코드해제 KPI
    var codeUnlockRequestCount: Int = 0,
    var codeUnlockApprovedCount: Int = 0,

    // 5. 종료된 채팅방 KPI
    var closedChatroomsCount: Int = 0,
    var avgChatDurationDays: BigDecimal = BigDecimal.ZERO,
) : BaseTimeEntity() {

    /**
     * 시그널 수락률 계산
     */
    fun getSignalAcceptanceRate(): BigDecimal {
        return if (signalSentCount > 0) {
            (signalAcceptedCount.toBigDecimal() / signalSentCount.toBigDecimal())
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    /**
     * 코드해제 승인률 계산
     */
    fun getCodeUnlockApprovalRate(): BigDecimal {
        return if (codeUnlockRequestCount > 0) {
            (codeUnlockApprovedCount.toBigDecimal() / codeUnlockRequestCount.toBigDecimal())
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }

    /**
     * 채팅방 활성률 계산 (활성 채팅방 / 현재 열려있는 채팅방)
     */
    fun getChatActivityRate(): BigDecimal {
        return if (currentOpenChatroomsCount > 0) {
            (activeChatroomsCount.toBigDecimal() / currentOpenChatroomsCount.toBigDecimal())
                .multiply(BigDecimal(100))
                .setScale(2, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
    }
}
