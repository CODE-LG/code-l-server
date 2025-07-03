package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
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

    @Query(
        value = "SELECT * FROM member WHERE id <> :excludeId AND member_status = 'DONE' ORDER BY RAND(:seed) LIMIT :randomSize",
        nativeQuery = true,
    )
    fun findRandomMembersStatusDone(
        @Param("excludeId") excludeId: Long,
        @Param("randomSize") randomSize: Long,
        @Param("recommendCode") seed: Long,
    ): List<Member>

    @Query(
        value = "SELECT * FROM member WHERE member_status = 'DONE' ORDER BY RAND(:seed) LIMIT :limit OFFSET :offset",
        nativeQuery = true,
    )
    fun findMembersWithSeedStatusDone(
        @Param("seed") seed: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int,
    ): List<Member>
}
