package codel.member.presentation.response

import codel.member.domain.MemberStatus

/**
 * 프로필 거절 정보 조회 응답
 */
data class ProfileRejectionInfoResponse(
    val status: MemberStatus,
    val hasFaceImageRejection: Boolean,
    val hasCodeImageRejection: Boolean,
    val rejectedFaceImages: List<RejectedImageDto>,
    val rejectedCodeImages: List<RejectedImageDto>
)

/**
 * 거절된 이미지 정보
 */
data class RejectedImageDto(
    val imageId: Long,
    val url: String,
    val order: Int,
    val rejectionReason: String
)
