package codel.member.presentation.response

/**
 * 프로필 이미지 조회 응답
 */
data class ProfileImagesResponse(
    val faceImages: List<ProfileImageDto>,
    val codeImages: List<ProfileImageDto>
)

/**
 * 프로필 이미지 정보
 */
data class ProfileImageDto(
    val imageId: Long,
    val url: String,
    val order: Int,
    val isApproved: Boolean,
    val rejectionReason: String?
)
