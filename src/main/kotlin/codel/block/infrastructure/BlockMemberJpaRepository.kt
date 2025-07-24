package codel.block.infrastructure

import codel.block.domain.BlockMember
import org.springframework.data.jpa.repository.JpaRepository

interface BlockMemberJpaRepository : JpaRepository<BlockMember, Long> {
}