package codel.member.infrastructure

import codel.member.infrastructure.entity.RejectReasonEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RejectReasonRepository : JpaRepository<RejectReasonEntity, Long>
