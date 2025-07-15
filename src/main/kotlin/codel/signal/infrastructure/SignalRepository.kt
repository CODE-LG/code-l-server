package codel.signal.infrastructure

import codel.signal.domain.Signal
import codel.member.domain.Member
import codel.signal.domain.SignalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface SignalRepository : JpaRepository<Signal, Long> {
    fun findTopByFromMemberAndToMemberOrderByIdDesc(fromMember: Member, toMember: Member): Signal?

    @Query("SELECT s FROM Signal s JOIN FETCH s.fromMember fm JOIN FETCH fm.profile WHERE s.toMember = :member AND s.status= :status")
    fun findByToMemberAndStatus(member: Member, @Param("status") signalStatus : SignalStatus) : List<Signal>
} 