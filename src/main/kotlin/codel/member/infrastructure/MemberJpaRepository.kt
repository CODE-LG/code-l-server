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
            OR LOWER(m.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(p.codeName) LIKE LOWER(CONCAT('%', :keyword, '%'))
          )
        """
    )
    fun findMembersWithFilterAdvanced(
        @Param("keyword") keyword: String?,
        @Param("status") status: MemberStatus?,
        pageable: Pageable
    ): Page<Member>

    fun countByMemberStatus(status: MemberStatus): Long


    @Query("SELECT m FROM Member m JOIN FETCH m.profile WHERE m.id = :memberId")
    fun findByMemberId(memberId: Long) : Member?

    @EntityGraph(attributePaths = ["profile", "profile.representativeQuestion"])
    @Query("select m from Member m where m.id = :id")
    fun findMemberWithProfileAndQuestion(@Param("id") id: Long): Member?

    // ========== 통계용 쿼리 ==========
    
    @Query("""
        SELECT DATE(m.createdAt) as date, COUNT(m) as count
        FROM Member m 
        WHERE m.createdAt >= :startDate
        GROUP BY DATE(m.createdAt)
        ORDER BY DATE(m.createdAt) DESC
    """)
    fun getDailySignupStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT m.memberStatus, COUNT(m) 
        FROM Member m 
        GROUP BY m.memberStatus
    """)
    fun getMemberStatusStats(): List<Array<Any>>
    
    @Query("""
        SELECT EXTRACT(YEAR FROM m.createdAt) as year, 
               EXTRACT(MONTH FROM m.createdAt) as month, 
               COUNT(m) as count
        FROM Member m 
        WHERE m.createdAt >= :startDate
        GROUP BY EXTRACT(YEAR FROM m.createdAt), EXTRACT(MONTH FROM m.createdAt)
        ORDER BY year DESC, month DESC
    """)
    fun getMonthlySignupStats(@Param("startDate") startDate: LocalDateTime): List<Array<Any>>
    
    @Query("""
        SELECT COUNT(m) 
        FROM Member m 
        WHERE DATE(m.createdAt) = CURRENT_DATE
    """)
    fun getTodaySignupCount(): Long
    
    @Query("""
        SELECT COUNT(m) 
        FROM Member m 
        WHERE m.createdAt >= :startDate
    """)
    fun getRecentSignupCount(@Param("startDate") startDate: LocalDateTime): Long
}
