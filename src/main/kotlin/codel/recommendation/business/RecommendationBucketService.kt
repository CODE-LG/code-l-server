package codel.recommendation.business

import codel.member.domain.Member
import codel.member.infrastructure.MemberJpaRepository
import codel.recommendation.domain.RecommendationConfig
import org.springframework.stereotype.Service
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.transaction.annotation.Transactional
import org.hibernate.Hibernate

/**
 * 추천 시스템의 4단계 버킷 정책을 구현하는 서비스
 * 
 * 버킷 정책 우선순위:
 * - B1: 동일 subRegion (최우선)
 * - B2: 동일 mainRegion 내 다른 subRegion
 * - B3: 인접 mainRegion 
 * - B4: 전국 범위 (최후 보충)
 */
@Service
@Transactional
class RecommendationBucketService(
    private val memberJpaRepository: MemberJpaRepository,
    private val recommendationConfig: RecommendationConfig
) {
    
    private val logger = KotlinLogging.logger {}
    
    /**
     * 4단계 버킷 정책에 따라 추천 후보자들을 추출합니다.
     * 상위 버킷에서 부족한 인원을 하위 버킷에서 보충합니다.
     * 
     * @param userMainRegion 사용자의 메인 지역 (예: 서울, 경기)
     * @param userSubRegion 사용자의 서브 지역 (예: 강남, 분당)
     * @param excludeIds 제외할 사용자 ID 목록 (자신, 차단, 중복 방지 대상)
     * @param requiredCount 필요한 추천 인원수
     * @return 버킷 정책에 따라 정렬된 추천 후보자 목록
     */
    fun getCandidatesByBucket(
        userMainRegion: String,
        userSubRegion: String,
        excludeIds: Set<Long>,
        requiredCount: Int
    ): List<Member> {
        
        logger.info { "버킷 정책 시작 - userRegion: $userMainRegion-$userSubRegion, excludeIds: ${excludeIds.size}개, requiredCount: $requiredCount" }
        
        val results = mutableListOf<Member>()
        val usedIds = mutableSetOf<Long>()
        usedIds.addAll(excludeIds)
        
        // B1: 동일 subRegion (최우선)
        if (results.size < requiredCount) {
            val b1Candidates = getBucket1Candidates(userMainRegion, userSubRegion, usedIds)
            val b1Selected = b1Candidates.take(requiredCount - results.size)
            results.addAll(b1Selected)
            usedIds.addAll(b1Selected.mapNotNull { it.id })
            
            logger.info { "B1 버킷 (동일 subRegion): ${b1Selected.size}명 선택 (전체 후보: ${b1Candidates.size}명)" }
        }
        
        // B2: 동일 mainRegion 내 다른 subRegion  
        if (results.size < requiredCount) {
            val b2Candidates = getBucket2Candidates(userMainRegion, userSubRegion, usedIds)
            val b2Selected = b2Candidates.take(requiredCount - results.size)
            results.addAll(b2Selected)
            usedIds.addAll(b2Selected.mapNotNull { it.id })
            
            logger.info { "B2 버킷 (동일 mainRegion): ${b2Selected.size}명 선택 (전체 후보: ${b2Candidates.size}명)" }
        }
        
        // B3: 인접 mainRegion
        if (results.size < requiredCount) {
            val b3Candidates = getBucket3Candidates(userMainRegion, usedIds)
            val b3Selected = b3Candidates.take(requiredCount - results.size)
            results.addAll(b3Selected)
            usedIds.addAll(b3Selected.mapNotNull { it.id })
            
            logger.info { "B3 버킷 (인접 mainRegion): ${b3Selected.size}명 선택 (전체 후보: ${b3Candidates.size}명)" }
        }
        
        // B4: 전국 범위 (최후 보충)
        if (results.size < requiredCount) {
            val b4Candidates = getBucket4Candidates(usedIds)
            val b4Selected = b4Candidates.take(requiredCount - results.size)
            results.addAll(b4Selected)
            
            logger.info { "B4 버킷 (전국 범위): ${b4Selected.size}명 선택 (전체 후보: ${b4Candidates.size}명)" }
        }
        
        logger.info { "버킷 정책 완료 - 최종 선택: ${results.size}명 / 요청: ${requiredCount}명" }
        
        return results
    }
    
    /**
     * B1 버킷: 동일한 subRegion을 가진 후보자들 조회
     * 가장 가까운 지역으로 최우선 추천
     */
    private fun getBucket1Candidates(
        mainRegion: String, 
        subRegion: String, 
        excludeIds: Set<Long>
    ): List<Member> {
        return memberJpaRepository.findByMainRegionAndSubRegionAndStatusDone(
            mainRegion = mainRegion,
            subRegion = subRegion,
            excludeIds = excludeIds.ifEmpty { setOf(0L) } // 빈 Set은 쿼리에서 문제가 될 수 있음
        )
    }
    
    /**
     * B2 버킷: 동일한 mainRegion이지만 다른 subRegion을 가진 후보자들 조회
     * 같은 광역시/도 내에서 확장 추천
     */
    private fun getBucket2Candidates(
        mainRegion: String, 
        excludeSubRegion: String, 
        excludeIds: Set<Long>
    ): List<Member> {
        return memberJpaRepository.findByMainRegionAndNotSubRegionAndStatusDone(
            mainRegion = mainRegion,
            excludeSubRegion = excludeSubRegion,
            excludeIds = excludeIds.ifEmpty { setOf(0L) }
        )
    }
    
    /**
     * B3 버킷: 인접한 mainRegion들의 후보자들 조회
     * 지리적으로 인접한 지역으로 확장 추천
     */
    private fun getBucket3Candidates(
        userMainRegion: String, 
        excludeIds: Set<Long>
    ): List<Member> {
        val adjacentRegions = RecommendationConfig.getAdjacentRegions(userMainRegion)
        
        return if (adjacentRegions.isNotEmpty()) {
            memberJpaRepository.findByAdjacentMainRegionsAndStatusDone(
                adjacentRegions = adjacentRegions,
                excludeIds = excludeIds.ifEmpty { setOf(0L) }
            )
        } else {
            logger.warn { "인접 지역이 없는 mainRegion: $userMainRegion (제주도 등)" }
            emptyList()
        }
    }
    
    /**
     * B4 버킷: 전국 범위에서 랜덤하게 후보자들 조회
     * 최후 보충용으로 공정한 랜덤 추천
     */
    private fun getBucket4Candidates(excludeIds: Set<Long>): List<Member> {
        return memberJpaRepository.findByStatusDoneExcludingIds(
            excludeIds = excludeIds.ifEmpty { setOf(0L) }
        )
    }
    
    /**
     * 버킷별 후보자 수를 미리 확인하는 유틸리티 메서드
     * 디버깅 및 모니터링 용도
     */
    fun getBucketStatistics(
        userMainRegion: String,
        userSubRegion: String,
        excludeIds: Set<Long>
    ): Map<String, Int> {
        val stats = mutableMapOf<String, Int>()
        val usedIds = excludeIds.toMutableSet()
        
        // B1 통계
        val b1Count = getBucket1Candidates(userMainRegion, userSubRegion, usedIds).size
        stats["B1_동일subRegion"] = b1Count
        
        // B2 통계  
        val b2Count = getBucket2Candidates(userMainRegion, userSubRegion, usedIds).size
        stats["B2_동일mainRegion"] = b2Count
        
        // B3 통계
        val b3Count = getBucket3Candidates(userMainRegion, usedIds).size
        stats["B3_인접mainRegion"] = b3Count
        
        // B4 통계 (샘플링으로 추정)
        val b4Sample = getBucket4Candidates(usedIds).take(100)
        stats["B4_전국범위_샘플"] = b4Sample.size
        
        return stats
    }
    
    /**
     * ID 목록으로부터 Member 객체들을 조회합니다.
     * 추천 이력에서 실제 Member 객체를 가져올 때 사용합니다.
     * 
     * @param memberIds 조회할 Member ID 목록
     * @return ID 순서대로 정렬된 Member 목록 (존재하지 않는 ID는 제외)
     */
    fun getMembersByIds(memberIds: List<Long>): List<Member> {
        if (memberIds.isEmpty()) {
            return emptyList()
        }
        
        // ID 목록으로 Member들 조회
        val members = memberJpaRepository.findAllByIdsWithProfileAndQuestion(memberIds)
        
        // Lazy Loading 강제 초기화 (Hibernate.initialize 사용)
        members.forEach { member ->
            Hibernate.initialize(member.profile)
            member.profile?.let { profile ->
                Hibernate.initialize(profile.representativeQuestion)
            }
        }

        val membersMap = members.associateBy { it.getIdOrThrow() }

        // 원본 순서 유지하면서 존재하는 Member만 반환
        val result = memberIds.mapNotNull { id ->
            membersMap[id]
        }
        
        if (result.size != memberIds.size) {
            logger.warn { 
                "일부 Member ID가 존재하지 않음 - requested: ${memberIds.size}개, " +
                "found: ${result.size}개, missing: ${memberIds - result.map { it.getIdOrThrow() }.toSet()}" 
            }
        }
        
        logger.debug { 
            "Member ID 목록 조회 완료 - requested: ${memberIds.size}개, " +
            "found: ${result.size}개" 
        }
        
        return result
    }
}
