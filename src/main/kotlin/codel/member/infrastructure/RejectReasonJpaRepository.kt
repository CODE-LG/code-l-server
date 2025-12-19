package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.RejectReason
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RejectReasonJpaRepository : JpaRepository<RejectReason, Long> {
    fun findByMember(member: Member): RejectReason?
}
