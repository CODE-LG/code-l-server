package codel.signal.infrastructure

import codel.signal.domain.Signal
import codel.member.domain.Member
import codel.signal.domain.SignalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SignalJpaRepository : JpaRepository<Signal, Long> {
    fun findTopByFromMemberAndToMemberOrderByIdDesc(fromMember: Member, toMember: Member): Signal?

    @Query("SELECT s FROM Signal s JOIN FETCH s.fromMember fm JOIN FETCH fm.profile WHERE s.toMember = :member AND s.status= :status")
    fun findByToMemberAndStatus(member: Member, @Param("status") signalStatus : SignalStatus) : List<Signal>

    @Query("SELECT s FROM Signal s JOIN FETCH s.toMember tm JOIN FETCH tm.profile WHERE s.fromMember = :member AND s.status= :status")
    fun findByFromMemberAndStatus(me: Member, @Param("status") signalStatus: SignalStatus) : List<Signal>

    @Query("""
    SELECT s.toMember.id FROM Signal s
    WHERE s.fromMember = :fromMember
      AND s.toMember IN :candidates
      AND (
        (s.status = 'REJECTED' AND s.createdAt >= :sevenDaysAgo)
        OR s.status IN ('ACCEPTED', 'ACCEPTED_HIDDEN', 'PENDING', 'PENDING_HIDDEN')
      )
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.fromMember = :fromMember AND s2.toMember IN :candidates
        GROUP BY s2.toMember
      )
    """)
    fun findExcludedToMemberIds(
        @Param("fromMember") fromMember: Member,
        @Param("candidates") candidates: List<Member>,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime
    ): List<Long>
} 