package codel.signal.infrastructure

import codel.member.domain.Member
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface SignalJpaRepository : JpaRepository<Signal, Long> {
    fun findTopByFromMemberAndToMemberOrderByIdDesc(fromMember: Member, toMember: Member): Signal?

    @Query("SELECT s FROM Signal s JOIN FETCH s.fromMember fm JOIN FETCH fm.profile WHERE s.toMember = :member AND s.senderStatus= :status")
    fun findByToMemberAndStatus(member: Member, @Param("status") signalStatus: SignalStatus): List<Signal>

    @Query(
        """
    SELECT DISTINCT s FROM Signal s
        JOIN FETCH s.fromMember fm
        JOIN FETCH fm.profile
        JOIN FETCH s.toMember tm
        JOIN FETCH tm.profile
        WHERE s.senderStatus = :status
        AND (s.fromMember = :member OR s.toMember = :member)
    """
    )
    fun findByMemberAndStatus(member: Member, @Param("status") signalStatus: SignalStatus): List<Signal>

    @Query(
        """
    SELECT DISTINCT s FROM Signal s
        JOIN FETCH s.fromMember fm
        JOIN FETCH fm.profile
        JOIN FETCH s.toMember tm
        JOIN FETCH tm.profile
        WHERE s.fromMember = :member
        AND s.senderStatus = :status
        AND NOT EXISTS (
            SELECT 1 FROM BlockMemberRelation b
            WHERE b.blockerMember = :member AND b.blockedMember.id = tm.id
            AND b.status = 'BLOCKED'
        )
    """
    )
    fun findByFromMemberAndStatus(member: Member, @Param("status") signalStatus: SignalStatus): List<Signal>

    @Query(
        """
    SELECT s.toMember.id FROM Signal s
    WHERE s.fromMember = :fromMember
      AND s.toMember IN :candidates
      AND (
        (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo)
        OR s.senderStatus IN ('ACCEPTED', 'ACCEPTED_HIDDEN', 'PENDING', 'PENDING_HIDDEN')
      )
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.fromMember = :fromMember AND s2.toMember IN :candidates
        GROUP BY s2.toMember
      )
    """
    )
    fun findExcludedToMemberIds(
        @Param("fromMember") fromMember: Member,
        @Param("candidates") candidates: List<Member>,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime
    ): List<Long>

    @Query(
        """
    SELECT s.toMember.id FROM Signal s
    WHERE s.fromMember = :fromMember
      AND s.toMember IN :candidates
      AND s.senderStatus IN ('ACCEPTED', 'ACCEPTED_HIDDEN')
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.fromMember = :fromMember AND s2.toMember IN :candidates
        GROUP BY s2.toMember
      )
    """
    )
    fun findAcceptedToMemberIds(
        @Param("fromMember") fromMember: Member,
        @Param("candidates") candidates: List<Member>
    ): List<Long>

    @Query(
        """
    SELECT s.toMember.id FROM Signal s
    WHERE s.fromMember = :fromMember
      AND s.toMember IN :candidates
      AND (
        (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo AND s.updatedAt < :todayMidnight)
        OR (s.senderStatus IN ('ACCEPTED', 'ACCEPTED_HIDDEN', 'PENDING', 'PENDING_HIDDEN') AND s.updatedAt < :todayMidnight)
      )
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.fromMember = :fromMember AND s2.toMember IN :candidates
        GROUP BY s2.toMember
      )
    """
    )
    fun findExcludedToMemberIdsAtMidnight(
        @Param("fromMember") fromMember: Member,
        @Param("candidates") candidates: List<Member>,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime,
        @Param("todayMidnight") todayMidnight: LocalDateTime
    ): List<Long>
} 