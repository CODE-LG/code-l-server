package codel.signal.infrastructure

import codel.signal.domain.Signal
import codel.member.domain.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SignalRepository : JpaRepository<Signal, Long> {
    fun findTopByFromMemberAndToMemberOrderByIdDesc(fromMember: Member, toMember: Member): Signal?
} 