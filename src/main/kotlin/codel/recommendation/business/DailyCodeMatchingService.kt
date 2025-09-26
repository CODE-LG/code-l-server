package codel.recommendation.business

import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 오늘의 코드매칭 서비스
 * 
 * 주요 기능:
 * - 24시간 유지되는 일일 추천 시스템
 * - 지역 기반 버킷 정책 적용
 * - 기존 추천 결과 재사용 (성능 최적화)
 * - 중복 방지 정책 적용
 */
@Service
@Transactional
class DailyCodeMatchingService(
    private val config: RecommendationConfig,
    private val bucketService: RecommendationBucketService,
    private val historyService: RecommendationHistoryService
) : Loggable {
    
    /**
     * 오늘의 코드매칭 추천을 수행합니다.
     * 
     * 동작 원리:
     * 1. 기존 오늘 추천 이력 확인 (24시간 유지)
     * 2. 기존 이력이 있으면 재사용 (성능 최적화)
     * 3. 없으면 새로 생성 (버킷 정책 + 중복 방지)
     * 4. 생성된 추천 결과를 이력에 저장
     * 
     * @param user 추천을 받을 사용자
     * @return 추천된 사용자 목록 (Member 객체)
     */
    fun getDailyCodeMatching(user: Member): List<Member> {
        log.info { "오늘의 코드매칭 요청 - userId: ${user.getIdOrThrow()}" }
        
        // 1. 기존 오늘 추천 결과 확인
        val existingRecommendationIds = historyService.getTodayDailyCodeMatchingIds(user)
        
        if (existingRecommendationIds.isNotEmpty()) {
            log.info { 
                "기존 오늘의 코드매칭 결과 재사용 - userId: ${user.getIdOrThrow()}, " +
                "count: ${existingRecommendationIds.size}개" 
            }
            
            // 기존 추천 결과를 Member 객체로 변환하여 반환
            return bucketService.getMembersByIds(existingRecommendationIds)
        }
        
        // 2. 새로운 추천 생성
        val newRecommendations = generateNewDailyCodeMatching(user)
        
        // 3. 추천 이력 저장
        if (newRecommendations.isNotEmpty()) {
            historyService.saveRecommendationHistory(
                user = user,
                recommendedUsers = newRecommendations,
                type = RecommendationType.DAILY_CODE_MATCHING,
                timeSlot = null,
                dateTime = LocalDateTime.now()
            )
        }
        
        log.info { 
            "새 오늘의 코드매칭 생성 완료 - userId: ${user.getIdOrThrow()}, " +
            "count: ${newRecommendations.size}개" 
        }
        
        return newRecommendations
    }
    
    /**
     * 새로운 오늘의 코드매칭 추천을 생성합니다.
     * 
     * @param user 추천을 받을 사용자
     * @return 새로 생성된 추천 사용자 목록
     */
    private fun generateNewDailyCodeMatching(user: Member): List<Member> {
        // 1. 사용자 지역 정보 확인
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
        
        // 2. 중복 방지 대상 조회
        val excludeIds = getExcludeIdsForDailyCodeMatching(user)
        
        log.debug { 
            "오늘의 코드매칭 생성 - userId: ${user.getIdOrThrow()}, " +
            "region: $userMainRegion-$userSubRegion, excludeCount: ${excludeIds.size}개" 
        }
        
        // 3. 버킷 정책으로 후보자 조회
        val candidates = bucketService.getCandidatesByBucket(
            userMainRegion = userMainRegion,
            userSubRegion = userSubRegion ?: "",
            excludeIds = excludeIds,
            requiredCount = config.dailyCodeCount
        )
        
        log.info { 
            "오늘의 코드매칭 후보자 선정 - userId: ${user.getIdOrThrow()}, " +
            "requested: ${config.dailyCodeCount}개, actual: ${candidates.size}개" 
        }
        
        return candidates
    }
    
    /**
     * 오늘의 코드매칭에서 제외해야 할 사용자 ID를 조회합니다.
     * 중복 방지 정책에 따라 결정됩니다.
     * 
     * @param user 기준이 되는 사용자
     * @return 제외해야 할 사용자 ID Set (본인 포함)
     */
    private fun getExcludeIdsForDailyCodeMatching(user: Member): Set<Long> {
        val excludeIds = mutableSetOf<Long>()
        
        // 1. 본인 제외
        excludeIds.add(user.getIdOrThrow())
        
        // 2. allowDuplicate 설정에 따른 중복 방지
        if (config.allowDuplicate) {
            // 타입간 중복 허용: 전체 추천 이력에서 제외
            excludeIds.addAll(historyService.getExcludedUserIds(user))
        } else {
            // 타입간 중복 비허용: 해당 타입만 제외
            excludeIds.addAll(historyService.getExcludedUserIdsByType(user, RecommendationType.DAILY_CODE_MATCHING))
        }
        
        return excludeIds
    }
    
    /**
     * 오늘의 코드매칭 이력이 있는지 확인합니다.
     * 
     * @param user 확인할 사용자
     * @param date 확인할 날짜 (기본값: 오늘)
     * @return 해당 날짜에 오늘의 코드매칭 이력이 있는지 여부
     */
    fun hasTodayDailyCodeMatching(user: Member, date: LocalDate = LocalDate.now()): Boolean {
        return historyService.hasRecommendationHistory(user, RecommendationType.DAILY_CODE_MATCHING, date)
    }
    
    /**
     * 오늘의 코드매칭을 강제로 새로 생성합니다.
     * 관리자용 기능이나 테스트 목적으로 사용합니다.
     * 
     * @param user 추천을 받을 사용자
     * @return 새로 생성된 추천 사용자 목록
     */
    fun forceRefreshDailyCodeMatching(user: Member): List<Member> {
        log.info { "오늘의 코드매칭 강제 새로고침 - userId: ${user.getIdOrThrow()}" }
        
        val newRecommendations = generateNewDailyCodeMatching(user)
        
        if (newRecommendations.isNotEmpty()) {
            historyService.saveRecommendationHistory(
                user = user,
                recommendedUsers = newRecommendations,
                type = RecommendationType.DAILY_CODE_MATCHING,
                timeSlot = null,
                dateTime = LocalDateTime.now()
            )
        }
        
        log.info { 
            "오늘의 코드매칭 강제 새로고침 완료 - userId: ${user.getIdOrThrow()}, " +
            "count: ${newRecommendations.size}개" 
        }
        
        return newRecommendations
    }
    
    /**
     * 오늘의 코드매칭 통계 정보를 조회합니다.
     * 모니터링 및 분석 목적으로 사용합니다.
     * 
     * @param user 통계를 조회할 사용자
     * @return 통계 정보 맵
     */
    fun getDailyCodeMatchingStats(user: Member): Map<String, Any> {
        val stats = mutableMapOf<String, Any>()
        
        // 1. 오늘 추천 여부
        val hasToday = hasTodayDailyCodeMatching(user)
        stats["hasToday"] = hasToday
        
        // 2. 오늘 추천된 사용자 수
        val todayRecommendationIds = historyService.getTodayDailyCodeMatchingIds(user)
        stats["todayCount"] = todayRecommendationIds.size
        
        // 3. 설정된 추천 목표 수
        stats["targetCount"] = config.dailyCodeCount
        
        // 4. 총 추천받은 고유 사용자 수
        stats["totalUniqueCount"] = historyService.getTotalUniqueRecommendedCount(user)
        
        // 5. 사용자 지역 정보
        val userProfile = user.profile
        if (userProfile != null) {
            stats["userRegion"] = "${userProfile.bigCity ?: "미설정"}-${userProfile.smallCity ?: "미설정"}"
        } else {
            stats["userRegion"] = "미설정"
        }
        
        log.debug { 
            "오늘의 코드매칭 통계 조회 - userId: ${user.getIdOrThrow()}, " +
            "stats: $stats" 
        }
        
        return stats
    }
}
