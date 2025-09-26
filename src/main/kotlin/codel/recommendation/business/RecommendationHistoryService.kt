package codel.recommendation.business

import codel.config.Loggable
import codel.member.domain.Member
import codel.recommendation.domain.RecommendationConfig
import codel.recommendation.domain.RecommendationHistory
import codel.recommendation.domain.RecommendationType
import codel.recommendation.infrastructure.RecommendationHistoryJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 추천 이력 저장 및 조회를 담당하는 서비스
 * 
 * 주요 기능:
 * - 추천 결과 이력 저장 (배치 처리 최적화)
 * - 중복 방지를 위한 제외 대상 조회
 * - 추천 타입별 이력 관리 (오늘의 코드매칭 vs 코드타임)
 * - 성능 최적화를 위한 배치 저장
 */
@Service
@Transactional
class RecommendationHistoryService(
    private val recommendationHistoryJpaRepository: RecommendationHistoryJpaRepository,
    private val config: RecommendationConfig
) : Loggable{
    
    /**
     * 추천 이력을 배치로 저장합니다.
     * 트랜잭션 안전성을 보장하며, 성능을 위해 saveAll()을 사용합니다.
     * 
     * @param user 추천을 받은 사용자
     * @param recommendedUsers 추천된 사용자들 목록
     * @param type 추천 타입 (DAILY_CODE_MATCHING or CODE_TIME)
     * @param timeSlot 코드타임의 경우 시간대 정보 (예: "10:00", "22:00")
     * @param dateTime 추천 시각 (기본값: 현재 시각)
     */
    fun saveRecommendationHistory(
        user: Member,
        recommendedUsers: List<Member>,
        type: RecommendationType,
        timeSlot: String? = null,
        dateTime: LocalDateTime = LocalDateTime.now()
    ) {
        if (recommendedUsers.isEmpty()) {
            log.warn { "추천 사용자 목록이 비어있습니다 - userId: ${user.getIdOrThrow()}, type: $type" }
            return
        }
        
        val histories = recommendedUsers.map { recommendedUser ->
            RecommendationHistory(
                user = user,
                recommendedUser = recommendedUser,
                recommendedAt = dateTime,
                recommendationType = type,
                recommendationTimeSlot = timeSlot
            )
        }
        
        try {
            recommendationHistoryJpaRepository.saveAll(histories)
            log.info {
                "추천 이력 저장 완료 - userId: ${user.getIdOrThrow()}, " +
                "type: $type, timeSlot: $timeSlot, count: ${histories.size}개" 
            }
        } catch (e: Exception) {
            log.error(e) {
                "추천 이력 저장 실패 - userId: ${user.getIdOrThrow()}, " +
                "type: $type, count: ${histories.size}개" 
            }
            throw e
        }
    }
    
    /**
     * 중복 방지를 위해 제외해야 할 사용자 ID 목록을 조회합니다.
     * repeatAvoidDays 설정에 따라 최근 N일 내 추천받은 사용자들을 제외합니다.
     * 
     * @param user 기준이 되는 사용자
     * @return 제외해야 할 사용자 ID Set
     */
    fun getExcludedUserIds(user: Member): Set<Long> {
        val fromDateTime = LocalDateTime.now().minusDays(config.repeatAvoidDays.toLong())
        
        val excludedIds = recommendationHistoryJpaRepository.findRecommendedUserIdsInPeriod(
            user = user,
            fromDateTime = fromDateTime
        ).toSet()
        
        log.debug {
            "중복 방지 대상 조회 - userId: ${user.getIdOrThrow()}, " +
            "period: ${config.repeatAvoidDays}일, excludedCount: ${excludedIds.size}개" 
        }
        
        return excludedIds
    }
    
    /**
     * 특정 추천 타입에 대한 제외 대상을 조회합니다.
     * allowDuplicate 설정에 따라 타입별 중복 허용 여부를 결정할 때 사용합니다.
     * 
     * @param user 기준이 되는 사용자
     * @param type 추천 타입
     * @return 해당 타입에서 제외해야 할 사용자 ID Set
     */
    fun getExcludedUserIdsByType(user: Member, type: RecommendationType): Set<Long> {
        val fromDateTime = LocalDateTime.now().minusDays(config.repeatAvoidDays.toLong())
        
        val excludedIds = recommendationHistoryJpaRepository.findRecommendedUserIdsByTypeInPeriod(
            user = user,
            type = type,
            fromDateTime = fromDateTime
        ).toSet()
        
        log.debug {
            "타입별 중복 방지 대상 조회 - userId: ${user.getIdOrThrow()}, " +
            "type: $type, excludedCount: ${excludedIds.size}개" 
        }
        
        return excludedIds
    }
    
    /**
     * 오늘의 코드매칭에서 추천받은 사용자 ID 목록을 조회합니다.
     * 24시간 유지 정책을 위해 기존 추천 결과를 재사용할 때 사용합니다.
     * 
     * @param user 추천을 받은 사용자
     * @return 오늘 추천받은 사용자 ID 목록 (생성 순서대로 정렬)
     */
    fun getTodayDailyCodeMatchingIds(user: Member): List<Long> {
        val today = LocalDate.now()
        
        val recommendedIds = recommendationHistoryJpaRepository.findTodayDailyCodeMatchingIds(
            user = user,
            today = today
        )
        
        log.debug {
            "오늘의 코드매칭 이력 조회 - userId: ${user.getIdOrThrow()}, " +
            "today: $today, count: ${recommendedIds.size}개" 
        }
        
        return recommendedIds
    }
    
    /**
     * 특정 시간대 코드타임에서 추천받은 사용자 ID 목록을 조회합니다.
     * 시간대별 추천 결과를 재사용할 때 사용합니다.
     * 
     * @param user 추천을 받은 사용자
     * @param timeSlot 시간대 (예: "10:00", "22:00")
     * @return 해당 시간대에 추천받은 사용자 ID 목록 (생성 순서대로 정렬)
     */
    fun getTodayCodeTimeIds(user: Member, timeSlot: String): List<Long> {
        val today = LocalDate.now()
        
        val recommendedIds = recommendationHistoryJpaRepository.findTodayCodeTimeIdsBySlot(
            user = user,
            timeSlot = timeSlot,
            today = today
        )
        
        log.debug {
            "코드타임 이력 조회 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, today: $today, count: ${recommendedIds.size}개" 
        }
        
        return recommendedIds
    }
    
    /**
     * 추천 이력이 존재하는지 확인합니다.
     * 추천 생성 여부를 판단할 때 사용합니다.
     * 
     * @param user 추천을 받은 사용자
     * @param type 추천 타입
     * @param date 확인할 날짜 (기본값: 오늘)
     * @return 해당 날짜에 해당 타입의 추천 이력이 있는지 여부
     */
    fun hasRecommendationHistory(
        user: Member, 
        type: RecommendationType, 
        date: LocalDate = LocalDate.now()
    ): Boolean {
        val hasHistory = recommendationHistoryJpaRepository.existsByUserAndTypeAndDate(
            user = user,
            type = type,
            date = date
        )
        
        log.debug {
            "추천 이력 존재 확인 - userId: ${user.getIdOrThrow()}, " +
            "type: $type, date: $date, exists: $hasHistory" 
        }
        
        return hasHistory
    }
    
    /**
     * 특정 시간대 코드타임 이력이 존재하는지 확인합니다.
     * 
     * @param user 추천을 받은 사용자
     * @param timeSlot 시간대 (예: "10:00", "22:00")
     * @param date 확인할 날짜 (기본값: 오늘)
     * @return 해당 날짜의 해당 시간대에 코드타임 추천 이력이 있는지 여부
     */
    fun hasCodeTimeHistory(
        user: Member, 
        timeSlot: String, 
        date: LocalDate = LocalDate.now()
    ): Boolean {
        val hasHistory = recommendationHistoryJpaRepository.existsByUserAndTimeSlotAndDate(
            user = user,
            timeSlot = timeSlot,
            date = date
        )
        
        log.debug {
            "코드타임 이력 존재 확인 - userId: ${user.getIdOrThrow()}, " +
            "timeSlot: $timeSlot, date: $date, exists: $hasHistory" 
        }
        
        return hasHistory
    }
    
    /**
     * 사용자별 총 추천받은 고유 사용자 수를 조회합니다.
     * 통계 및 분석 목적으로 사용합니다.
     * 
     * @param user 기준이 되는 사용자
     * @return 지금까지 추천받은 고유 사용자 수
     */
    fun getTotalUniqueRecommendedCount(user: Member): Long {
        val count = recommendationHistoryJpaRepository.countUniqueRecommendedUsers(user)
        
        log.debug {
            "총 추천받은 고유 사용자 수 조회 - userId: ${user.getIdOrThrow()}, count: $count" 
        }
        
        return count
    }
    
    /**
     * 특정 사용자의 모든 추천 이력을 삭제합니다.
     * 회원 탈퇴 시 개인정보 보호를 위해 사용합니다.
     * 
     * @param user 삭제할 사용자
     * @return 삭제된 이력 수
     */
    fun deleteAllHistoryForUser(user: Member): Long {
        val deletedCount = recommendationHistoryJpaRepository.deleteByUser(user) + 
                          recommendationHistoryJpaRepository.deleteByRecommendedUser(user)
        
        log.info {
            "사용자 추천 이력 삭제 완료 - userId: ${user.getIdOrThrow()}, " +
            "deletedCount: ${deletedCount}개" 
        }
        
        return deletedCount
    }
    
    /**
     * 오래된 추천 이력을 정리합니다.
     * 배치 작업에서 사용하여 데이터베이스 크기를 관리합니다.
     * 
     * @param cutoffDays 보관 기간 (일) - 이보다 오래된 이력 삭제
     * @return 삭제된 이력 수
     */
    fun cleanupOldHistories(cutoffDays: Int = 90): Long {
        val cutoffDateTime = LocalDateTime.now().minusDays(cutoffDays.toLong())
        
        val deletedCount = recommendationHistoryJpaRepository.deleteByRecommendedAtBefore(cutoffDateTime)
        
        log.info {
            "오래된 추천 이력 정리 완료 - cutoffDays: ${cutoffDays}일, " +
            "cutoffDateTime: $cutoffDateTime, deletedCount: ${deletedCount}개" 
        }
        
        return deletedCount
    }
}
