package codel.member.presentation.response

import io.swagger.v3.oas.annotations.media.Schema

/**
 * 프로필 이미지 조회 응답
 */
@Schema(description = "프로필 이미지 조회 응답")
data class ProfileImagesResponse(
    @Schema(description = "얼굴 이미지 목록")
    val faceImages: List<ProfileImageDto>,
    
    @Schema(description = "코드 이미지 목록")
    val codeImages: List<ProfileImageDto>
)

/**
 * 프로필 이미지 정보
 */
@Schema(description = "프로필 이미지 상세 정보")
data class ProfileImageDto(
    @Schema(description = "이미지 ID", example = "101")
    val imageId: Long,
    
    @Schema(description = "이미지 URL", example = "https://example.com/face1.jpg")
    val url: String,
    
    @Schema(description = "이미지 순서 (1부터 시작)", example = "1")
    val order: Int,
    
    @Schema(description = "승인 여부", example = "true")
    val isApproved: Boolean,
    
    @Schema(description = "거절 사유 (거절된 경우만)", example = "얼굴이 명확하게 보이지 않습니다", nullable = true)
    val rejectionReason: String?
)
