package codel.member.infrastructure

import codel.member.domain.FaceImage
import org.springframework.data.jpa.repository.JpaRepository

interface FaceImageRepository : JpaRepository<FaceImage, Long> {
    fun findByProfileIdOrderByOrder(profileId: Long): List<FaceImage>
    fun findByProfileIdAndIsApprovedFalse(profileId: Long): List<FaceImage>
    fun deleteByProfileId(profileId: Long)
}
