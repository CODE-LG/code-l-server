package codel.block.infrastructure

import codel.block.domain.BlockMemberRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface BlockMemberRelationJpaRepository : JpaRepository<BlockMemberRelation, Long> {

    @Query("SELECT bmr FROM BlockMemberRelation bmr WHERE bmr.blockerMember.id = :blockerId AND bmr.status = 'BLOCKED'")
    fun findBlockMembersBy(blockerId: Long) : List<BlockMemberRelation>

    @Query("SELECT bmr FROM BlockMemberRelation bmr WHERE bmr.blockedMember.id = :blockedId AND bmr.status = 'BLOCKED'")
    fun findBlockerMembersTo(blockedId: Long) : List<BlockMemberRelation>

    @Query("SELECT bmr FROM BlockMemberRelation bmr WHERE bmr.blockerMember.id = :blockerMemberId AND bmr.blockedMember.id = :blockedMemberId")
    fun findByBlockerMemberAndBlockedMember(blockerMemberId : Long, blockedMemberId: Long) : BlockMemberRelation?

    @Query(
        """
        SELECT bmr.blockedMember.id FROM BlockMemberRelation bmr
        WHERE bmr.blockerMember.id = :blockerId
          AND bmr.status = 'BLOCKED'
          AND bmr.createdAt < :beforeTime
        """
    )
    fun findBlockedMemberIdByMeBeforeTime(
        @Param("blockerId") blockerId: Long,
        @Param("beforeTime") beforeTime: LocalDateTime
    ): List<Long>

    @Query(
        """
        SELECT bmr.blockerMember.id FROM BlockMemberRelation bmr
        WHERE bmr.blockedMember.id = :blockedId
          AND bmr.status = 'BLOCKED'
          AND bmr.createdAt < :beforeTime
        """
    )
    fun findBlockMembersByOtherBeforeTime(
        @Param("blockedId") blockedId: Long,
        @Param("beforeTime") beforeTime: LocalDateTime
    ): List<Long>
}