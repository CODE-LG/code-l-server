package codel.block.infrastructure

import codel.block.domain.BlockMemberRelation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BlockMemberRelationJpaRepository : JpaRepository<BlockMemberRelation, Long> {

    @Query("SELECT bmr FROM BlockMemberRelation bmr WHERE bmr.blockerMember.id = :blockerId")
    fun findBlockMembersBy(blockerId: Long) : List<BlockMemberRelation>

    fun findByBlockerMemberAndBlockedMember(blockerMemberId : Long, blockedMemberId: Long) : BlockMemberRelation?
}