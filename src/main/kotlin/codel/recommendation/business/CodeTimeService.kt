package codel.recommendation.business

import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.domain.CodeTimeRecommendationResult
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 코드타임 서비스
 *
 * 주요 기능:
 * - 시간대별 추천 시스템 (10:00, 22:00)
 * - 각 시간대마다 독립적인 추천 결과
 * - 지역 기반 버킷 정책 적용
 * - 시간대별 추천 결과 재사용 (성능 최적화)
 * - 중복 방지 정책 적용
 */
@Service
@Transactional
class CodeTimeService(
    private val config: RecommendationConfig,
    private val bucketService: RecommendationBucketService,
    private val historyService: RecommendationHistoryService,
    private val exclusionService: RecommendationExclusionService
) : Loggable {

    /**
     * 코드타임 추천을 수행합니다.
     * 
     * 동작:
     * - 현재 시간대(10:00 또는 22:00) 확인
     * - 해당 시간대의 유효 기간 내 추천 이력 확인
     * - 이력이 있으면 실시간 필터링 후 반환
     * - 없으면 새로 생성
     */
    fun getCodeTimeRecommendation(user: Member,
                                  page : Int,
                                  size : Int
    ): Page<Member> {
        val timeSlotCalculator = TimeSlotCalculator("ko")
        log.info { "코드타임 추천 요청 - userId: ${user.getIdOrThrow()}" }

        // 1. 현재 시간대 확인 (항상 "10:00" 또는 "22:00" 반환)
        val currentTimeSlot = timeSlotCalculator.getCurrentTimeSlot()

        log.debug {
            "현재 시간대: $currentTimeSlot - currentTime: ${LocalTime.now()}"
        }

        // 2. 현재 시간대의 유효 기간 계산
        val (startDateTime, endDateTime) = timeSlotCalculator.getValidRangeFor(currentTimeSlot)
        
        log.debug {
            "유효 기간: $startDateTime ~ $endDateTime"
        }

        // 3. 유효 기간 내 추천 이력 확인
        val existingRecommendationIds = historyService.getCodeTimeIdsByTimeRange(
            user = user,
            timeSlot = currentTimeSlot,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )

        if (existingRecommendationIds.isNotEmpty()) {
            log.info {
                "기존 코드타임 결과 존재 - userId: ${user.getIdOrThrow()}, " +
                        "timeSlot: $currentTimeSlot, count: ${existingRecommendationIds.size}개"
            }

            // 4. 실시간 필터링: 차단/시그널 등으로 제외해야 할 사용자 제외
            val filteredIds = filterExcludedMembers(user, existingRecommendationIds)

            if (filteredIds.size != existingRecommendationIds.size) {
                log.info {
                    "실시간 필터링 적용 - userId: ${user.getIdOrThrow()}, " +
                            "before: ${existingRecommendationIds.size}명, after: ${filteredIds.size}명, " +
                            "filtered: ${existingRecommendationIds.size - filteredIds.size}명"
                }
            }

            if (filteredIds.isNotEmpty()) {
                val members = bucketService.getMembersByIds(filteredIds)
                val pageable = PageRequest.of(page, size)

                return PageImpl(members, pageable, members.size.toLong())
            }

            // 모두 필터링되어 비어있으면 새로 생성
            log.info {
                "모든 추천이 필터링됨, 새로 생성 - userId: ${user.getIdOrThrow()}"
            }
        }

        // 5. 새로운 추천 생성
        val newRecommendations = generateNewCodeTimeRecommendation(user, currentTimeSlot)

        // 6. 추천 이력 저장
        if (newRecommendations.isNotEmpty()) {
            historyService.saveRecommendationHistory(
                user = user,
                recommendedUsers = newRecommendations,
                type = RecommendationType.CODE_TIME,
                timeSlot = currentTimeSlot,
                dateTime = LocalDateTime.now()
            )
        }

        log.info {
            "새 코드타임 생성 완료 - userId: ${user.getIdOrThrow()}, " +
                    "timeSlot: $currentTimeSlot, count: ${newRecommendations.size}개"
        }

//        return CodeTimeRecommendationResult(
//            timeSlot = currentTimeSlot,
//            members = newRecommendations,
//            isActiveTime = true,
//            nextTimeSlot = getNextTimeSlot()
//        )
        val pageable = PageRequest.of(page, size)

        return PageImpl(newRecommendations, pageable, newRecommendations.size.toLong())

    }

    /**
     * 기존 추천 목록에서 실시간으로 제외해야 할 사용자를 필터링합니다.
     *
     * 제외 대상:
     * - 차단한 사용자
     * - 나를 차단한 사용자
     * - 최근 시그널 보낸 사용자
     * - WITHDRAWN 상태의 사용자 (회원 탈퇴)
     *
     * @param user 기준 사용자
     * @param memberIds 필터링할 사용자 ID 목록
     * @return 필터링된 사용자 ID 목록
     */
    private fun filterExcludedMembers(user: Member, memberIds: List<Long>): List<Long> {
        if (memberIds.isEmpty()) {
            return emptyList()
        }

        val excludeIds = mutableSetOf<Long>()
        excludeIds.addAll(exclusionService.getBlockedMemberIds(user))
        excludeIds.addAll(exclusionService.getRecentSignalMemberIds(user))

        // WITHDRAWN 상태의 회원 필터링
        // getMembersByIds를 통해 조회하면 자동으로 WITHDRAWN이 제외됨
        val validMembers = bucketService.getMembersByIds(memberIds)
        val validIds = validMembers.map { it.getIdOrThrow() }

        val filteredIds = validIds.filter { it !in excludeIds }

        log.debug {
            "실시간 필터링 - userId: ${user.getIdOrThrow()}, " +
                    "original: ${memberIds.size}명, excluded: ${excludeIds.size}명, " +
                    "withdrawn: ${memberIds.size - validIds.size}명, " +
                    "result: ${filteredIds.size}명"
        }

        return filteredIds
    }

    /**
     * 특정 시간대의 코드타임 추천을 조회합니다.
     */
    fun getCodeTimeRecommendationBySlot(
        user: Member,
        timeSlot: String,
        date: LocalDate = LocalDate.now()
    ): CodeTimeRecommendationResult {
        val timeSlotCalculator = TimeSlotCalculator("ko")
        log.info {
            "특정 시간대 코드타임 조회 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, date: $date"
        }

        // 유효한 시간대인지 확인
        if (timeSlot !in config.codeTimeSlots) {
            log.warn {
                "유효하지 않은 시간대 - userId: ${user.getIdOrThrow()}, " +
                "timeSlot: $timeSlot, validSlots: ${config.codeTimeSlots}"
            }
            return CodeTimeRecommendationResult(
                timeSlot = timeSlot,
                members = emptyList(),
                isActiveTime = false,
                nextTimeSlot = getNextTimeSlot()
            )
        }

        // 해당 시간대 추천 결과 조회
        val recommendationIds = if (date == LocalDate.now()) {
            historyService.getTodayCodeTimeIds(user, timeSlot)
        } else {
            emptyList()
        }

        val members = if (recommendationIds.isNotEmpty()) {
            bucketService.getMembersByIds(recommendationIds)
        } else {
            emptyList()
        }

        val isCurrentTimeSlot = timeSlotCalculator.getCurrentTimeSlot() == timeSlot

        return CodeTimeRecommendationResult(
            timeSlot = timeSlot,
            members = members,
            isActiveTime = isCurrentTimeSlot,
            nextTimeSlot = getNextTimeSlot()
        )
    }

    private fun generateNewCodeTimeRecommendation(user: Member, timeSlot: String): List<Member> {
        val userProfile = user.profile
        if (userProfile == null) {
            log.warn { "사용자 프로필이 없습니다 - userId: ${user.getIdOrThrow()}" }
            return emptyList()
        }

        val userMainRegion = userProfile.bigCity
        val userSubRegion = userProfile.smallCity

        if (userMainRegion.isNullOrBlank()) {
            log.warn { "사용자 지역 정보가 없습니다 - userId: ${user.getIdOrThrow()}" }
            return emptyList()
        }

        val excludeIds = getExcludeIdsForCodeTime(user)

        log.debug {
            "코드타임 생성 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, region: $userMainRegion-$userSubRegion, " +
            "excludeCount: ${excludeIds.size}개"
        }

        val candidates = bucketService.getCandidatesByBucket(
            userMainRegion = userMainRegion,
            userSubRegion = userSubRegion ?: "",
            excludeIds = excludeIds,
            requiredCount = config.codeTimeCount
        )

        log.info {
            "코드타임 후보자 선정 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, requested: ${config.codeTimeCount}개, actual: ${candidates.size}개"
        }

        return candidates
    }

    private fun getExcludeIdsForCodeTime(user: Member): Set<Long> {
        return exclusionService.getAllExcludedIds(user, RecommendationType.CODE_TIME)
    }

    fun getNextTimeSlot(): String? {
        // 간단한 하드코딩된 로직으로 임시 해결
        val currentTime = LocalTime.now()
        val currentHour = currentTime.hour

        return when {
            currentHour < 10 -> "10:00"
            currentHour < 22 -> "22:00"
            else -> "10:00" // 다음날 10:00
        }
    }

    fun hasCodeTimeHistory(
        user: Member,
        timeSlot: String,
        date: LocalDate = LocalDate.now()
    ): Boolean {
        return historyService.hasCodeTimeHistory(user, timeSlot, date)
    }

    fun forceRefreshCodeTime(user: Member, timeSlot: String): CodeTimeRecommendationResult {
        val timeSlotCalculator = TimeSlotCalculator("ko")
        log.info {
            "코드타임 강제 새로고침 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot"
        }

        if (timeSlot !in config.codeTimeSlots) {
            log.warn {
                "유효하지 않은 시간대로 강제 새로고침 시도 - userId: ${user.getIdOrThrow()}, " +
                "timeSlot: $timeSlot, validSlots: ${config.codeTimeSlots}"
            }
            return CodeTimeRecommendationResult.createEmptyActiveResult(timeSlot, getNextTimeSlot())
        }

        val newRecommendations = generateNewCodeTimeRecommendation(user, timeSlot)

        if (newRecommendations.isNotEmpty()) {
            historyService.saveRecommendationHistory(
                user = user,
                recommendedUsers = newRecommendations,
                type = RecommendationType.CODE_TIME,
                timeSlot = timeSlot,
                dateTime = LocalDateTime.now()
            )
        }

        log.info {
            "코드타임 강제 새로고침 완료 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, count: ${newRecommendations.size}개"
        }

        return CodeTimeRecommendationResult(
            timeSlot = timeSlot,
            members = newRecommendations,
            isActiveTime = timeSlotCalculator.getCurrentTimeSlot() == timeSlot,
            nextTimeSlot = getNextTimeSlot()
        )
    }

    fun getCodeTimeStats(user: Member): Map<String, Any> {
        val timeSlotCalculator = TimeSlotCalculator("ko")
        val stats = mutableMapOf<String, Any>()

        val currentTimeSlot = timeSlotCalculator.getCurrentTimeSlot()
        stats["currentTimeSlot"] = currentTimeSlot ?: ""
        stats["isActiveTime"] = currentTimeSlot != null
        stats["nextTimeSlot"] = getNextTimeSlot() ?: ""

        stats["configuredTimeSlots"] = config.codeTimeSlots
        stats["targetCountPerSlot"] = config.codeTimeCount

        val slotStats = mutableMapOf<String, Map<String, Any>>()
        for (timeSlot in config.codeTimeSlots) {
            val hasHistory = hasCodeTimeHistory(user, timeSlot)
            val recommendationIds = historyService.getTodayCodeTimeIds(user, timeSlot)

            slotStats[timeSlot] = mapOf(
                "hasHistory" to hasHistory,
                "recommendationCount" to recommendationIds.size,
                "isCurrentSlot" to (currentTimeSlot == timeSlot)
            )
        }
        stats["slotStatistics"] = slotStats

        stats["totalUniqueCount"] = historyService.getTotalUniqueRecommendedCount(user)

        val userProfile = user.profile
        if (userProfile != null) {
            stats["userRegion"] = "${userProfile.bigCity ?: "미설정"}-${userProfile.smallCity ?: "미설정"}"
        } else {
            stats["userRegion"] = "미설정"
        }

        log.debug {
            "코드타임 통계 조회 - userId: ${user.getIdOrThrow()}, " +
            "stats: $stats"
        }

        return stats
    }

    fun getAllTodayCodeTimeResults(user: Member): Map<String, CodeTimeRecommendationResult> {
        val results = mutableMapOf<String, CodeTimeRecommendationResult>()

        for (timeSlot in config.codeTimeSlots) {
            results[timeSlot] = getCodeTimeRecommendationBySlot(user, timeSlot)
        }

        log.debug {
            "전체 코드타임 결과 조회 - userId: ${user.getIdOrThrow()}, " +
            "slotsCount: ${results.size}"
        }

        return results
    }
}
