package codel.member.infrastructure

import codel.member.infrastructure.entity.MemberEntity
import codel.member.infrastructure.entity.RejectReasonEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RejectReasonJpaRepository : JpaRepository<RejectReasonEntity, Long> {
    fun findByMemberEntity(memberEntity: MemberEntity): RejectReasonEntity?
}
