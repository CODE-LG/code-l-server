package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

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

    fun countByMemberStatus(memberStatus: MemberStatus): Long

    @Query(
        value = "SELECT * FROM member WHERE id <> :excludeId AND member_status = 'DONE' ORDER BY RAND(:seed) LIMIT :randomSize",
        nativeQuery = true,
    )
    fun findRandomMembersStatusDone(
        @Param("excludeId") excludeId: Long,
        @Param("randomSize") randomSize: Long,
        @Param("seed") seed: Long,
    ): List<Member>

    @Query(
        value = "SELECT * FROM member WHERE id <> :excludeId AND member_status = 'DONE' ORDER BY RAND(:seed) LIMIT :limit OFFSET :offset",
        nativeQuery = true,
    )
    fun findMembersWithSeedStatusDoneExcludeMe(
        @Param("excludeId") excludeId: Long,
        @Param("seed") seed: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<Member>

    @Query(
        value = "SELECT COUNT(*) FROM member WHERE id <> :excludeId AND member_status = 'DONE'",
        nativeQuery = true,
    )
    fun countMembersStatusDoneExcludeMe(
        @Param("excludeId") excludeId: Long,
    ): Long

    @Query(
        value = "SELECT m FROM Member m JOIN FETCH m.profile WHERE m.id <> :excludeId AND m.memberStatus = 'DONE' ORDER BY function('RAND', :seed)",
        countQuery = "SELECT COUNT(m) FROM Member m WHERE m.id <> :excludeId AND m.memberStatus = 'DONE'"
    )
    fun findRandomMembersStatusDoneWithProfile(
        @Param("excludeId") excludeId: Long,
        @Param("seed") seed: Long,
        pageRequest: PageRequest
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


    @Query("SELECT m FROM Member m JOIN FETCH m.profile WHERE m.id = :memberId")
    fun findByMemberId(memberId: Long) : Member?
}
