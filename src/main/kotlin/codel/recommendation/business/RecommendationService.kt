package codel.recommendation.business

import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.domain.CodeTimeRecommendationResult
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import codel.member.business.MemberService
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * 통합 추천 서비스
 *
 * 주요 기능:
 * - 오늘의 코드매칭과 코드타임을 통합 관리
 * - 사용자 상황에 맞는 최적의 추천 제공
 * - 기존 MemberService와의 연동점 역할
 * - 추천 시스템 전체 상태 모니터링
 */
@Service
@Transactional(readOnly = true)
class RecommendationService(
    private val dailyCodeMatchingService: DailyCodeMatchingService,
    private val codeTimeService: CodeTimeService,
    private val bucketService: RecommendationBucketService,
    private val historyService: RecommendationHistoryService,
    private val memberService: MemberService,
    private val exclusionService: RecommendationExclusionService,
    private val config: RecommendationConfig
) : Loggable {

    /**
     * 사용자의 현재 상황에 맞는 추천을 제공합니다.
     *
     * 우선순위:
     * 1. 현재 코드타임 활성화 시간대면 → 코드타임 추천
     * 2. 코드타임 비활성 시간대면 → 오늘의 코드매칭 추천
     * 3. 둘 다 있으면 사용자 선택에 따라 반환
     *
     * @param user 추천을 받을 사용자
     * @param preferCodeTime 코드타임을 우선적으로 원하는지 여부 (기본: false)
     * @return 통합 추천 결과
     */
    @Transactional(readOnly = false)
    fun getRecommendation(user: Member, preferCodeTime: Boolean = false): RecommendationResult {
        log.info { "통합 추천 요청 - userId: ${user.getIdOrThrow()}, preferCodeTime: $preferCodeTime" }

        val currentTimeSlot = codeTimeService.getCurrentTimeSlot()
        val isCodeTimeActive = currentTimeSlot != null

        return when {
            // 1. 코드타임 활성 시간대이고 사용자가 코드타임을 선호하는 경우
            isCodeTimeActive && preferCodeTime -> {
                val codeTimeResult = codeTimeService.getCodeTimeRecommendation(user)
                val dailyResult = dailyCodeMatchingService.getDailyCodeMatching(user)

                RecommendationResult(
                    primaryRecommendation = PrimaryRecommendation.CODE_TIME,
                    codeTimeResult = codeTimeResult,
                    dailyCodeMatching = dailyResult,
                    recommendationMessage = "현재 코드타임이 활성화되어 있습니다! (${currentTimeSlot})"
                )
            }

            // 2. 코드타임 활성 시간대이지만 오늘의 코드매칭을 선호하는 경우
            isCodeTimeActive && !preferCodeTime -> {
                val dailyResult = dailyCodeMatchingService.getDailyCodeMatching(user)
                val codeTimeResult = codeTimeService.getCodeTimeRecommendation(user)

                RecommendationResult(
                    primaryRecommendation = PrimaryRecommendation.DAILY_CODE_MATCHING,
                    codeTimeResult = codeTimeResult,
                    dailyCodeMatching = dailyResult,
                    recommendationMessage = "오늘의 코드매칭을 추천드립니다! (코드타임도 ${currentTimeSlot}에 이용 가능)"
                )
            }

            // 3. 코드타임 비활성 시간대 → 오늘의 코드매칭 위주
            !isCodeTimeActive -> {
                val dailyResult = dailyCodeMatchingService.getDailyCodeMatching(user)
                val nextTimeSlot = codeTimeService.getNextTimeSlot()

                RecommendationResult(
                    primaryRecommendation = PrimaryRecommendation.DAILY_CODE_MATCHING,
                    codeTimeResult = null,
                    dailyCodeMatching = dailyResult,
                    recommendationMessage = if (nextTimeSlot != null) {
                        "오늘의 코드매칭을 추천드립니다! 다음 코드타임은 ${nextTimeSlot}입니다."
                    } else {
                        "오늘의 코드매칭을 추천드립니다!"
                    }
                )
            }

            else -> {
                // 예외 상황: 기본적으로 오늘의 코드매칭 제공
                val dailyResult = dailyCodeMatchingService.getDailyCodeMatching(user)

                RecommendationResult(
                    primaryRecommendation = PrimaryRecommendation.DAILY_CODE_MATCHING,
                    codeTimeResult = null,
                    dailyCodeMatching = dailyResult,
                    recommendationMessage = "오늘의 코드매칭을 추천드립니다!"
                )
            }
        }
    }

    /**
     * 오늘의 코드매칭만 조회합니다.
     * 기존 MemberService와의 호환성을 위한 메서드입니다.
     *
     * @param user 추천을 받을 사용자
     * @return 오늘의 코드매칭 결과
     */
    @Transactional
    fun getDailyCodeMatching(user: Member): List<Member> {
        log.info { "오늘의 코드매칭 단독 요청 - userId: ${user.getIdOrThrow()}" }
        return dailyCodeMatchingService.getDailyCodeMatching(user)
    }

    /**
     * 코드타임만 조회합니다.
     *
     * @param user 추천을 받을 사용자
     * @return 코드타임 결과
     */
    @Transactional(readOnly = false)
    fun getCodeTime(user: Member): CodeTimeRecommendationResult {
        log.info { "코드타임 단독 요청 - userId: ${user.getIdOrThrow()}" }
        return codeTimeService.getCodeTimeRecommendation(user)
    }

    /**
     * 특정 시간대의 코드타임을 조회합니다.
     *
     * @param user 추천을 받을 사용자
     * @param timeSlot 조회할 시간대
     * @return 해당 시간대의 코드타임 결과
     */
    fun getCodeTimeBySlot(user: Member, timeSlot: String): CodeTimeRecommendationResult {
        log.info {
            "특정 시간대 코드타임 요청 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot"
        }
        return codeTimeService.getCodeTimeRecommendationBySlot(user, timeSlot)
    }

    /**
     * 사용자의 추천 현황을 종합적으로 조회합니다.
     *
     * @param user 조회할 사용자
     * @return 종합 추천 현황
     */
    fun getRecommendationOverview(user: Member): RecommendationOverview {
        log.info { "추천 현황 종합 조회 - userId: ${user.getIdOrThrow()}" }

        // 1. 오늘의 코드매칭 현황
        val hasDailyCodeMatching = dailyCodeMatchingService.hasTodayDailyCodeMatching(user)
        val dailyStats = dailyCodeMatchingService.getDailyCodeMatchingStats(user)

        // 2. 코드타임 현황
        val codeTimeStats = codeTimeService.getCodeTimeStats(user)
        val allCodeTimeResults = codeTimeService.getAllTodayCodeTimeResults(user)

        // 3. 현재 상태
        val currentTimeSlot = codeTimeService.getCurrentTimeSlot()
        val nextTimeSlot = codeTimeService.getNextTimeSlot()

        // 4. 버킷 통계 (선택사항)
        val userProfile = user.profile
        val bucketStats = if (userProfile?.bigCity != null) {
            bucketService.getBucketStatistics(
                userMainRegion = userProfile.bigCity!!,
                userSubRegion = userProfile.smallCity ?: "",
                excludeIds = historyService.getExcludedUserIds(user)
            )
        } else {
            emptyMap()
        }

        return RecommendationOverview(
            userId = user.getIdOrThrow(),
            hasDailyCodeMatching = hasDailyCodeMatching,
            currentTimeSlot = currentTimeSlot,
            nextTimeSlot = nextTimeSlot,
            isCodeTimeActive = currentTimeSlot != null,
            dailyCodeMatchingStats = dailyStats,
            codeTimeStats = codeTimeStats,
            allCodeTimeResults = allCodeTimeResults,
            bucketStatistics = bucketStats,
            totalUniqueRecommendationCount = historyService.getTotalUniqueRecommendedCount(user)
        )
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

    /**
     * 추천 강제 새로고침 (관리자용)
     *
     * @param user 대상 사용자
     * @param type 새로고침할 추천 타입
     * @param timeSlot 코드타임인 경우 시간대
     * @return 새로고침 결과
     */
    @Transactional(readOnly = false)
    fun forceRefreshRecommendation(
        user: Member,
        type: RecommendationType,
        timeSlot: String? = null
    ): Any {
        log.info {
            "추천 강제 새로고침 - userId: ${user.getIdOrThrow()}, " +
            "type: $type, timeSlot: $timeSlot"
        }

        return when (type) {
            RecommendationType.DAILY_CODE_MATCHING -> {
                dailyCodeMatchingService.forceRefreshDailyCodeMatching(user)
            }
            RecommendationType.CODE_TIME -> {
                if (timeSlot != null) {
                    codeTimeService.forceRefreshCodeTime(user, timeSlot)
                } else {
                    val currentSlot = codeTimeService.getCurrentTimeSlot()
                    if (currentSlot != null) {
                        codeTimeService.forceRefreshCodeTime(user, currentSlot)
                    } else {
                        log.warn { "코드타임 강제 새로고침 실패: 현재 활성 시간대가 없음 - userId: ${user.getIdOrThrow()}" }
                        throw IllegalStateException("현재 코드타임 활성 시간대가 아닙니다.")
                    }
                }
            }
        }
    }

    /**
     * 추천 이력 삭제 (회원 탈퇴 시 사용)
     *
     * @param user 삭제할 사용자
     * @return 삭제된 이력 수
     */
    @Transactional(readOnly = false)
    fun deleteAllRecommendationHistory(user: Member): Long {
        log.info { "추천 이력 전체 삭제 - userId: ${user.getIdOrThrow()}" }
        return historyService.deleteAllHistoryForUser(user)
    }

    /**
     * 시스템 헬스체크용 메서드
     *
     * @return 추천 시스템 상태 정보
     */
    fun getSystemHealthCheck(): Map<String, Any> {
        return mapOf(
            "systemStatus" to "HEALTHY",
            "currentTime" to LocalTime.now().toString(),
            "currentDate" to LocalDate.now().toString(),
            "activeTimeSlot" to (codeTimeService.getCurrentTimeSlot() ?: "NONE"),
            "nextTimeSlot" to (codeTimeService.getNextTimeSlot() ?: "NONE"),
            "configuredTimeSlots" to config.codeTimeSlots,
            "recommendationSettings" to getRecommendationSettings()
        )
    }

    /**
     * 제외 로직 통계 조회 (디버깅용)
     *
     * @param user 조회할 사용자
     * @param type 추천 타입
     * @return 제외 통계 정보
     */
    fun getExclusionStatistics(user: Member, type: RecommendationType): Map<String, Any> {
        log.info {
            "제외 통계 조회 - userId: ${user.getIdOrThrow()}, type: $type"
        }

        return exclusionService.getExclusionStatistics(user, type)
    }

    /**
     * 랜덤 추천 (파도타기) - 기존 MemberService와의 호환성 유지
     *
     * @param user 추천을 받을 사용자
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 랜덤 추천 결과 (페이징)
     */
    @Transactional(readOnly = false)
    fun getRandomMembers(user: Member, page: Int, size: Int): Page<Member> {
        log.info {
            "랜덤 추천 (파도타기) 요청 - userId: ${user.getIdOrThrow()}, " +
            "page: $page, size: $size"
        }

        return memberService.getRandomMembers(user, page, size)
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
