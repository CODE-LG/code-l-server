package codel.block.infrastructure

import codel.block.domain.BlockMember
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BlockMemberJpaRepository : JpaRepository<BlockMember, Long> {

    @Query("SELECT bm FROM BlockMember bm WHERE bm.blockerMember.id = :blockerId")
    fun findBlockMembersBy(blockerId: Long) : List<BlockMember>
}