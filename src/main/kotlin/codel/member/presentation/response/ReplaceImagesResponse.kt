package codel.member.presentation.response

import codel.member.domain.MemberStatus

/**
 * 이미지 교체 응답
 */
data class ReplaceImagesResponse(
    val uploadedCount: Int,
    val profileStatus: MemberStatus,
    val message: String
)
