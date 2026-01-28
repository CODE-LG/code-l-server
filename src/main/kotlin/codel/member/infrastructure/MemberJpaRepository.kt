package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface MemberJpaRepository : JpaRepository<Member, Long> {
    fun existsByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): Boolean

    fun findByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): Member

    fun findByMemberStatus(memberStatus: MemberStatus): List<Member>


    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m
        WHERE m.id <> :excludeId
          AND m.memberStatus = codel.member.domain.MemberStatus.DONE
        ORDER BY function('RAND', :seed)
    """)
    fun findRandomMembersStatusDone(
        @Param("excludeId") excludeId: Long,
        @Param("seed") seed: Long,
    ): List<Member>

    @Query(
        value = "SELECT * FROM member WHERE id <> :excludeId AND member_status = 'DONE'",
        nativeQuery = true,
    )
    fun findMembersWithStatusDoneExcludeMe(
        @Param("excludeId") excludeId: Long
    ): List<Member>

    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m
        WHERE m.id <> :excludeId
          AND m.memberStatus = 'DONE'
        ORDER BY function('RAND', :seed)
    """)
    fun findRandomMembersStatusDoneWithProfile(
        @Param("excludeId") excludeId: Long,
        @Param("seed") seed: Long,
    ): List<Member>

    @Query(
        """
        SELECT m FROM Member m JOIN FETCH m.profile p
        WHERE (:status IS NULL OR m.memberStatus = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        ORDER BY m.id DESC
        """
    )
    fun findMembersWithFilter(
        @Param("keyword") keyword: String?,
        @Param("status") status: MemberStatus?,
        pageable: Pageable
    ): Page<Member>

    @Query(
        """
        SELECT m FROM Member m JOIN FETCH m.profile p
        WHERE (:status IS NULL OR m.memberStatus = :status)
          AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
          AND (:startDate IS NULL OR m.createdAt >= :startDate)
          AND (:endDate IS NULL OR m.createdAt < :endDate)
        """
    )
    fun findMembersWithFilterAdvanced(
        @Param("keyword") keyword: String?,
        @Param("status") status: MemberStatus?,
        @Param("startDate") startDate: LocalDateTime?,
        @Param("endDate") endDate: LocalDateTime?,
        pageable: Pageable
    ): Page<Member>

    fun countByMemberStatus(status: MemberStatus): Long


    @Query("SELECT m FROM Member m JOIN FETCH m.profile WHERE m.id = :memberId")
    fun findByMemberId(memberId: Long) : Member?


    @EntityGraph(attributePaths = ["profile", "profile.codeImages"])
    @Query("select m from Member m where m.id = :memberId")
    fun findByMemberIdWithProfileAndCodeImages(memberId: Long) : Member?

    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("select m from Member m where m.id = :id")
    fun findMemberWithProfileAndQuestion(@Param("id") id: Long): Member?

    // ========== 통계용 쿼리 ==========
    
    @Query("""
        SELECT CAST(m.createdAt AS date) as date, COUNT(m) as count
        FROM Member m 
        WHERE m.createdAt >= :startDate
        GROUP BY CAST(m.createdAt AS date)
        ORDER BY CAST(m.createdAt AS date) DESC
    """)
    fun getDailySignupStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT m.memberStatus, COUNT(m) 
        FROM Member m 
        GROUP BY m.memberStatus
    """)
    fun getMemberStatusStats(): List<Array<Any>>
    
    @Query("""
        SELECT YEAR(m.createdAt) as year, 
               MONTH(m.createdAt) as month, 
               COUNT(m) as count
        FROM Member m 
        WHERE m.createdAt >= :startDate
        GROUP BY YEAR(m.createdAt), MONTH(m.createdAt)
        ORDER BY YEAR(m.createdAt) DESC, MONTH(m.createdAt) DESC
    """)
    fun getMonthlySignupStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT COUNT(m) 
        FROM Member m 
        WHERE YEAR(m.createdAt) = YEAR(CURRENT_DATE) 
        AND MONTH(m.createdAt) = MONTH(CURRENT_DATE)
        AND DAY(m.createdAt) = DAY(CURRENT_DATE)
    """)
    fun getTodaySignupCount(): Long
    
    @Query("""
        SELECT COUNT(m) 
        FROM Member m 
        WHERE m.createdAt >= :startDate
    """)
    fun getRecentSignupCount(@Param("startDate") startDate: LocalDateTime): Long
    
    // ========== 추천 시스템용 버킷 쿼리 ==========
    
    /**
     * B1 버킷: 동일한 mainRegion과 subRegion을 가진 사용자들 조회
     * 타이브레이커: 최근 접속순 → 가입일 최신순
     */
    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m JOIN m.profile p
        WHERE m.memberStatus = 'DONE'
          AND p.bigCity = :mainRegion
          AND p.smallCity = :subRegion
          AND m.id NOT IN :excludeIds
        ORDER BY m.updatedAt DESC, m.createdAt DESC
    """)
    fun findByMainRegionAndSubRegionAndStatusDone(
        @Param("mainRegion") mainRegion: String,
        @Param("subRegion") subRegion: String,
        @Param("excludeIds") excludeIds: Set<Long>
    ): List<Member>
    
    /**
     * B2 버킷: 동일한 mainRegion이지만 다른 subRegion을 가진 사용자들 조회
     * 타이브레이커: 최근 접속순 → 가입일 최신순
     */
    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m JOIN m.profile p
        WHERE m.memberStatus = 'DONE'
          AND p.bigCity = :mainRegion
          AND p.smallCity != :excludeSubRegion
          AND m.id NOT IN :excludeIds
        ORDER BY m.updatedAt DESC, m.createdAt DESC
    """)
    fun findByMainRegionAndNotSubRegionAndStatusDone(
        @Param("mainRegion") mainRegion: String,
        @Param("excludeSubRegion") excludeSubRegion: String,
        @Param("excludeIds") excludeIds: Set<Long>
    ): List<Member>
    
    /**
     * B3 버킷: 특정 mainRegion 목록에 속하는 사용자들 조회 (인접 지역)
     * 타이브레이커: 최근 접속순 → 가입일 최신순
     */
    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m JOIN m.profile p
        WHERE m.memberStatus = 'DONE'
          AND p.bigCity IN :adjacentRegions
          AND m.id NOT IN :excludeIds
        ORDER BY m.updatedAt DESC, m.createdAt DESC
    """)
    fun findByAdjacentMainRegionsAndStatusDone(
        @Param("adjacentRegions") adjacentRegions: List<String>,
        @Param("excludeIds") excludeIds: Set<Long>
    ): List<Member>
    
    /**
     * B4 버킷: 전국 범위에서 랜덤하게 사용자들 조회 (최후 보충)
     * 랜덤 정렬로 공정성 보장
     */
    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("""
        SELECT m
        FROM Member m JOIN m.profile p
        WHERE m.memberStatus = 'DONE'
          AND m.id NOT IN :excludeIds
        ORDER BY function('RAND')
    """)
    fun findByStatusDoneExcludingIds(
        @Param("excludeIds") excludeIds: Set<Long>
    ): List<Member>


    @Query("""
        SELECT DISTINCT m FROM Member m
        JOIN FETCH m.profile p
        LEFT JOIN FETCH p.representativeQuestion
        WHERE m.id IN :ids
    """)
    fun findAllByIdsWithProfileAndQuestion(@Param("ids") ids: List<Long>): List<Member>
    /**
     * 관리자용: 프로필만 Fetch Join으로 조회
     */
    @Query("""
        SELECT m
        FROM Member m
        LEFT JOIN FETCH m.profile p
        WHERE m.id = :memberId
    """)
    fun findMemberWithProfile(@Param("memberId") memberId: Long): Member?
}
