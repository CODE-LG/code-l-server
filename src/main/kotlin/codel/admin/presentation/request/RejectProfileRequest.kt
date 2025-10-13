package codel.admin.presentation.request

/**
 * 프로필 거절 요청 (관리자용)
 */
data class RejectProfileRequest(
    val faceImageRejections: List<ImageRejection>?,
    val codeImageRejections: List<ImageRejection>?
)

/**
 * 이미지 거절 정보
 */
data class ImageRejection(
    val imageId: Long,
    val reason: String
)
