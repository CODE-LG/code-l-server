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

    @Query("""
    SELECT s FROM Signal s
        JOIN FETCH s.fromMember fm
        JOIN FETCH fm.profile
        JOIN FETCH fm.profile.representativeQuestion
        JOIN FETCH s.toMember tm
        WHERE s.toMember = :member
        AND s.receiverStatus = :status
        AND NOT EXISTS (
            SELECT 1 FROM BlockMemberRelation b
            WHERE b.blockerMember = :member AND b.blockedMember.id = fm.id
            AND b.status = 'BLOCKED')
        """)
    fun findByToMemberAndStatus(member: Member, @Param("status") signalStatus: SignalStatus): List<Signal>
    @Query(
        """
    SELECT DISTINCT s FROM Signal s
        JOIN FETCH s.fromMember fm
        JOIN FETCH fm.profile
        JOIN FETCH fm.profile.representativeQuestion
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
        JOIN FETCH fm.profile.representativeQuestion
        JOIN FETCH s.toMember tm
        JOIN FETCH tm.profile
        JOIN FETCH tm.profile.representativeQuestion
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
    SELECT s.fromMember.id FROM Signal s
    WHERE s.toMember = :toMember
      AND s.fromMember IN :candidates
      AND (
        (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo AND s.updatedAt < :todayMidnight)
        OR (s.senderStatus IN ('ACCEPTED', 'PENDING', 'PENDING_HIDDEN') AND s.updatedAt < :todayMidnight)
      )
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.toMember = :toMember AND s2.fromMember IN :candidates
        GROUP BY s2.toMember
      )
    """
    )
    fun findExcludedFromMemberIdsAtMidnight(
        @Param("toMember") toMember: Member,
        @Param("candidates") candidates: List<Member>,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime,
        @Param("todayMidnight") todayMidnight: LocalDateTime
    ): List<Long>


    @Query(
        """
    SELECT s.toMember.id FROM Signal s
    WHERE s.fromMember = :fromMember
      AND s.fromMember IN :candidates
      AND (
        (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo AND s.updatedAt < :todayMidnight)
        OR (s.senderStatus IN ('ACCEPTED', 'PENDING', 'PENDING_HIDDEN') AND s.updatedAt < :todayMidnight)
      )
      AND s.id IN (
        SELECT MAX(s2.id) FROM Signal s2
        WHERE s2.fromMember = :fromMember AND s2.toMember IN :candidates
        GROUP BY s2.fromMember
      )
    """
    )
    fun findExcludedToMemberIdsAtMidnight(
        @Param("fromMember") fromMember: Member,
        @Param("candidates") candidates: List<Member>,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime,
        @Param("todayMidnight") todayMidnight: LocalDateTime
    ): List<Long>

    @Query(
        """
        SELECT s.fromMember.id
        FROM Signal s
        WHERE s.toMember = :toMember
          AND s.id = (
              SELECT MAX(s2.id)
              FROM Signal s2
              WHERE s2.toMember = :toMember
                AND s2.fromMember = s.fromMember
          )
          AND (
            (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo AND s.updatedAt < :todayMidnight)
            OR (s.senderStatus IN ('ACCEPTED', 'PENDING', 'PENDING_HIDDEN') AND s.updatedAt < :todayMidnight)
          )
        """
    )
    fun findExcludedFromMemberIdsAtMidnight(
        @Param("toMember") toMember: Member,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime,
        @Param("todayMidnight") todayMidnight: LocalDateTime
    ): List<Long>



    @Query(
        """
        SELECT s.toMember.id
        FROM Signal s
        WHERE s.fromMember = :fromMember
          AND s.id = (
              SELECT MAX(s2.id)
              FROM Signal s2
              WHERE s2.fromMember = :fromMember
                AND s2.toMember = s.toMember
          )
          AND (
            (s.senderStatus = 'REJECTED' AND s.updatedAt >= :sevenDaysAgo AND s.updatedAt < :todayMidnight)
            OR (s.senderStatus IN ('ACCEPTED', 'PENDING', 'PENDING_HIDDEN') AND s.updatedAt < :todayMidnight)
          )
        """
    )
    fun findExcludedToMemberIdsAtMidnight(
        @Param("fromMember") fromMember: Member,
        @Param("sevenDaysAgo") sevenDaysAgo: LocalDateTime,
        @Param("todayMidnight") todayMidnight: LocalDateTime
    ): List<Long>
} 