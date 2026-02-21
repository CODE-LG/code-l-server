package codel.recommendation.business

import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.domain.CodeTimeRecommendationResult
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import codel.member.business.MemberService
import codel.member.presentation.response.FullProfileResponse
import org.redisson.api.RedissonClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * 통합 추천 서비스
 *
 * 주요 기능:
 * - 오늘의 코드매칭과 코드타임을 통합 관리
 * - 사용자 상황에 맞는 최적의 추천 제공
 * - 기존 MemberService와의 연동점 역할
 * - 추천 시스템 전체 상태 모니터링
 *
 * 동시성 제어:
 * - Redisson 분산 락 + TransactionTemplate 조합으로 다중 서버 환경에서 중복 추천 차단
 * - 분산 락 보유 중에 트랜잭션 커밋까지 완료 보장
 */
@Service
class RecommendationService(
    private val dailyCodeMatchingService: DailyCodeMatchingService,
    private val codeTimeService: CodeTimeService,
    private val config: RecommendationConfig,
    private val transactionTemplate: TransactionTemplate,
    private val redissonClient: RedissonClient
) : Loggable {

    /**
     * 오늘의 코드매칭만 조회합니다.
     *
     * 동시성 제어:
     * - Redisson 분산 락으로 다중 서버 환경에서도 사용자별 중복 추천 방지
     * - 락 보유 중 트랜잭션 실행 및 커밋 → 락 해제 시점에 이미 DB 저장 완료
     * - leaseTime 10초: 고정 TTL로 스레드 행(hang) 시 무한 잠김 방지
     *
     * @param user 추천을 받을 사용자
     * @return 오늘의 코드매칭 결과
     */
    fun getDailyCodeMatching(user: Member): List<Member> {
        val userId = user.getIdOrThrow()
        log.info { "오늘의 코드매칭 요청 - userId: $userId" }

        val lock = redissonClient.getLock("recommendation:daily:$userId")
        lock.lock(10, TimeUnit.SECONDS)
        try {
            log.info { "분산 락 획득 성공, 트랜잭션 시작 - userId: $userId" }

            val result = transactionTemplate.execute {
                dailyCodeMatchingService.getDailyCodeMatching(user)
            } ?: emptyList()

            log.info { "트랜잭션 커밋 완료, 추천 ${result.size}명 - userId: $userId, members: ${result.map { it.getIdOrThrow() }}" }
            return result
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
            log.info { "분산 락 해제 - userId: $userId" }
        }
    }

    /**
     * 코드타임만 조회합니다.
     *
     * 동시성 제어:
     * - Redisson 분산 락으로 다중 서버 환경에서도 사용자별 중복 추천 방지
     * - leaseTime 10초: 고정 TTL로 스레드 행(hang) 시 무한 잠김 방지
     *
     * @param user 추천을 받을 사용자
     * @return 코드타임 결과 (DTO로 변환 완료)
     */
    fun getCodeTime(user: Member, page: Int, size: Int): Page<codel.member.presentation.response.FullProfileResponse> {
        val userId = user.getIdOrThrow()
        log.info { "코드타임 요청 - userId: $userId" }

        val lock = redissonClient.getLock("recommendation:codetime:$userId")
        lock.lock(10, TimeUnit.SECONDS)
        try {
            log.info { "분산 락 획득 성공, 트랜잭션 시작 - userId: $userId" }

            val result = transactionTemplate.execute {
                val memberPage = codeTimeService.getCodeTimeRecommendation(user, page, size)

                // 트랜잭션 내에서 DTO 변환 -> Lazy Loading 문제 해결
                memberPage.map { memberEntity ->
                    FullProfileResponse.createOpen(memberEntity)
                }
            } ?: PageImpl(emptyList())

            log.info { "트랜잭션 커밋 완료, 추천 ${result.content.size}명 - userId: $userId" }
            return result
        } finally {
            if (lock.isHeldByCurrentThread) {
                lock.unlock()
            }
            log.info { "분산 락 해제 - userId: $userId" }
        }
    }

    /**
     * 추천 시스템 전체 설정을 조회합니다.
     *
     * @return 현재 추천 시스템 설정
     */
    fun getRecommendationSettings(): Map<String, Any> {
        return mapOf(
            "dailyCodeCount" to config.dailyCodeCount,
            "codeTimeCount" to config.codeTimeCount,
            "codeTimeSlots" to config.codeTimeSlots,
            "dailyRefreshTime" to config.dailyRefreshTime,
            "repeatAvoidDays" to config.repeatAvoidDays,
            "allowDuplicate" to config.allowDuplicate,
            "currentTime" to LocalTime.now().toString(),
            "currentDate" to LocalDate.now().toString()
        )
    }
}

/**
 * 통합 추천 결과 데이터 클래스
 */
data class RecommendationResult(
    val primaryRecommendation: PrimaryRecommendation,
    val codeTimeResult: CodeTimeRecommendationResult?,
    val dailyCodeMatching: List<Member>,
    val recommendationMessage: String
)

/**
 * 주 추천 타입 enum
 */
enum class PrimaryRecommendation {
    DAILY_CODE_MATCHING,    // 오늘의 코드매칭 우선
    CODE_TIME               // 코드타임 우선
}

/**
 * 추천 현황 종합 데이터 클래스
 */
data class RecommendationOverview(
    val userId: Long,
    val hasDailyCodeMatching: Boolean,
    val currentTimeSlot: String?,
    val nextTimeSlot: String?,
    val isCodeTimeActive: Boolean,
    val dailyCodeMatchingStats: Map<String, Any>,
    val codeTimeStats: Map<String, Any>,
    val allCodeTimeResults: Map<String, CodeTimeRecommendationResult>,
    val bucketStatistics: Map<String, Int>,
    val totalUniqueRecommendationCount: Long
)
