package codel.verification.presentation.response

import codel.member.domain.MemberStatus
import codel.verification.domain.VerificationImage
import java.time.LocalDateTime

/**
 * 인증 이미지 제출 응답 DTO
 */
data class VerificationImageResponse(
    val memberId: Long,
    val memberStatus: MemberStatus,
    val verificationImage: VerificationImageDetail
) {
    data class VerificationImageDetail(
        val id: Long,
        val standardImageId: Long,
        val userImageUrl: String,
        val createdAt: LocalDateTime
    )

    companion object {
        fun from(memberId: Long, memberStatus: MemberStatus, verificationImage: VerificationImage): VerificationImageResponse {
            return VerificationImageResponse(
                memberId = memberId,
                memberStatus = memberStatus,
                verificationImage = VerificationImageDetail(
                    id = verificationImage.id!!,
                    standardImageId = verificationImage.standardVerificationImage.id!!,
                    userImageUrl = verificationImage.userImageUrl,
                    createdAt = verificationImage.createdAt
                )
            )
        }
    }
}
