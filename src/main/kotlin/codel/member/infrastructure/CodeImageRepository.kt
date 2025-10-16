package codel.member.infrastructure

import codel.member.domain.CodeImage
import org.springframework.data.jpa.repository.JpaRepository

interface CodeImageRepository : JpaRepository<CodeImage, Long> {
    fun findByProfileIdOrderByOrdersAsc(profileId: Long): List<CodeImage>
    fun findByProfileIdAndIsApprovedFalse(profileId: Long): List<CodeImage>
    fun deleteByProfileId(profileId: Long)
}
