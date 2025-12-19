package codel.member.presentation.response

import codel.member.domain.MemberStatus

/**
 * 코드 이미지 수정 응답
 */
data class UpdateCodeImagesResponse(
    val uploadedCount: Int,
    val profileStatus: MemberStatus,
    val message: String
)
