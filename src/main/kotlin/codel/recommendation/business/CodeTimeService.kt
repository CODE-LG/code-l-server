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
    private val exclusionService: RecommendationExclusionService,
    private val timeZoneService: TimeZoneService,
    private val agePreferenceResolver: AgePreferenceResolver
) : Loggable {

    /**
     * 코드타임 추천을 수행합니다.
     * 
     * 동작:
     * - 타임존 기준으로 현재 시간대(10:00 또는 22:00) 확인
     * - 해당 시간대의 유효 기간 내 추천 이력 확인
     * - 이력이 있으면 실시간 필터링 후 반환
     * - 없으면 새로 생성
     * 
     * @param user 추천을 받을 사용자
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @param timeZoneId 타임존 ID (null이면 기본값 KST 사용)
     */
    fun getCodeTimeRecommendation(user: Member,
                                  page : Int,
                                  size : Int,
                                  timeZoneId: String? = null
    ): Page<Member> {
        val timeSlotCalculator = TimeSlotCalculator("ko")
        log.info { "코드타임 추천 요청 - userId: ${user.getIdOrThrow()}" }

        // 1. 타임존 기준 현재 시간대 확인 (항상 "10:00" 또는 "22:00" 반환)
        val currentTimeSlot = timeZoneService.getCurrentTimeSlot(timeZoneId)
        
        log.debug {
            "현재 시간대: $currentTimeSlot - now: ${timeZoneService.getNow(timeZoneId)}"
        }

        // 2. 타임존 기준 현재 시간대의 유효 기간 계산 (UTC로 변환)
        val (startDateTime, endDateTime) = timeZoneService.getTimeSlotRangeInUTC(currentTimeSlot, timeZoneId)
        
        log.debug {
            "유효 기간(UTC): $startDateTime ~ $endDateTime"
        }

        // 3. 유효 기간 내 추천 이력 확인
        val existingRecommendationIds = historyService.getCodeTimeIdsByTimeRange(
            user = user,
            timeSlot = currentTimeSlot,
            startDateTime = startDateTime,
            endDateTime = endDateTime
        )
        log.info { "유효 기간 내 추천 이력 확인 : " + existingRecommendationIds.size }

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

            val pageable = PageRequest.of(page, size)

            return PageImpl(ArrayList(), pageable, 0L)
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

        val pageable = PageRequest.of(page, size)

        return PageImpl(newRecommendations, pageable, newRecommendations.size.toLong())

    }
    
    /**
     * 기존 추천 목록에서 실시간으로 제외해야 할 사용자를 필터링합니다.
     *
     * 제외 대상:
     * - 차단한 사용자
     * - 나를 차단한 사용자
     * - WITHDRAWN 상태의 사용자 (회원 탈퇴)
     *
     * 주의: 시그널 관계는 실시간 필터링에서 제외하지 않음
     * → 추천 세션 일관성 유지를 위해 새로운 추천 생성 시에만 제외
     *
     * @param user 기준 사용자
     * @param memberIds 필터링할 사용자 ID 목록
     * @return 필터링된 사용자 ID 목록
     */
    private fun filterExcludedMembers(user: Member, memberIds: List<Long>): List<Long> {
        if (memberIds.isEmpty()) {
            return emptyList()
        }

        // 실시간 제외 대상 조회 (차단만)
        val excludeIds = mutableSetOf<Long>()

        // 1. 차단 관계만 확인 (즉시 반영)
        excludeIds.addAll(exclusionService.getBlockedMemberIds(user))

        // 2. 시그널 관계는 확인하지 않음 (추천 세션 일관성 유지)
        // → 새로운 추천 생성 시에만 제외됨

        // 3. WITHDRAWN 상태의 회원 필터링
        // getMembersByIds를 통해 조회하면 자동으로 WITHDRAWN이 제외됨
        val validMembers = bucketService.getMembersByIds(memberIds)
        val validIds = validMembers.map { it.getIdOrThrow() }

        // 4. 최종 필터링
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
        date: LocalDate = LocalDate.now(),
        timeZoneId: String? = null
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
                nextTimeSlot = getNextTimeSlot(timeZoneId)
            )
        }

        // 해당 시간대 추천 결과 조회
        val today = timeZoneService.getToday(timeZoneId)
        val recommendationIds = if (date == today) {
            // 타임존 기준 오늘이면 시간 범위로 조회
            val (startDateTime, endDateTime) = timeZoneService.getTimeSlotRangeInUTC(timeSlot, timeZoneId)
            historyService.getCodeTimeIdsByTimeRange(user, timeSlot, startDateTime, endDateTime)
        } else {
            emptyList()
        }

        val members = if (recommendationIds.isNotEmpty()) {
            bucketService.getMembersByIds(recommendationIds)
        } else {
            emptyList()
        }

        val isCurrentTimeSlot = getCurrentTimeSlot(timeZoneId) == timeSlot

        return CodeTimeRecommendationResult(
            timeSlot = timeSlot,
            members = members,
            isActiveTime = isCurrentTimeSlot,
            nextTimeSlot = getNextTimeSlot(timeZoneId)
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

        // 나이 정보 조회
        val userAge = try {
            userProfile.getAge()
        } catch (e: Exception) {
            log.warn { "사용자 나이 정보 조회 실패 - userId: ${user.getIdOrThrow()}, 나이 필터링 없이 진행" }
            null
        }

        val agePreference = agePreferenceResolver.resolve(user)

        log.debug {
            "코드타임 생성 - userId: ${user.getIdOrThrow()}, " +
                "timeSlot: $timeSlot, region: $userMainRegion-$userSubRegion, userAge: $userAge, " +
                "agePreference: preferredMax=${agePreference.preferredMaxDiff}, cutoff=${agePreference.cutoffDiff}, " +
                "excludeCount: ${excludeIds.size}개"
        }

        val candidates = bucketService.getCandidatesByBucket(
            userMainRegion = userMainRegion,
            userSubRegion = userSubRegion ?: "",
            excludeIds = excludeIds,
            requiredCount = config.codeTimeCount,
            userAge = userAge,
            agePreference = agePreference
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

    /**
     * 타임존 기준으로 현재 활성 시간대를 반환합니다.
     * 
     * @param timeZoneId 타임존 ID (null이면 기본값 KST 사용)
     * @return 현재 활성 시간대 ("10:00" 또는 "22:00")
     */
    fun getCurrentTimeSlot(timeZoneId: String? = null): String {
        return timeZoneService.getCurrentTimeSlot(timeZoneId)
    }

    fun getNextTimeSlot(timeZoneId: String? = null): String? {
        val zone = timeZoneService.getTimeZone(timeZoneId)
        val now = java.time.LocalDateTime.now(zone)
        val currentHour = now.hour

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

    fun forceRefreshCodeTime(user: Member, timeSlot: String, timeZoneId: String? = null): CodeTimeRecommendationResult {
        log.info {
            "코드타임 강제 새로고침 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot"
        }

        if (timeSlot !in config.codeTimeSlots) {
            log.warn {
                "유효하지 않은 시간대로 강제 새로고침 시도 - userId: ${user.getIdOrThrow()}, " +
                "timeSlot: $timeSlot, validSlots: ${config.codeTimeSlots}"
            }
            return CodeTimeRecommendationResult.createEmptyActiveResult(timeSlot, getNextTimeSlot(timeZoneId))
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
            isActiveTime = getCurrentTimeSlot(timeZoneId) == timeSlot,
            nextTimeSlot = getNextTimeSlot(timeZoneId)
        )
    }

    fun getCodeTimeStats(user: Member, timeZoneId: String? = null): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()

        val currentTimeSlot = getCurrentTimeSlot(timeZoneId)
        stats["currentTimeSlot"] = currentTimeSlot
        stats["isActiveTime"] = true
        stats["nextTimeSlot"] = getNextTimeSlot(timeZoneId) ?: ""

        stats["configuredTimeSlots"] = config.codeTimeSlots
        stats["targetCountPerSlot"] = config.codeTimeCount

        val slotStats = mutableMapOf<String, Map<String, Any>>()
        for (timeSlot in config.codeTimeSlots) {
            val hasHistory = hasCodeTimeHistory(user, timeSlot)
            
            // 타임존 기준으로 오늘인지 확인
            val today = timeZoneService.getToday(timeZoneId)
            val recommendationIds = if (today == LocalDate.now()) {
                val (startDateTime, endDateTime) = timeZoneService.getTimeSlotRangeInUTC(timeSlot, timeZoneId)
                historyService.getCodeTimeIdsByTimeRange(user, timeSlot, startDateTime, endDateTime)
            } else {
                emptyList()
            }

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
