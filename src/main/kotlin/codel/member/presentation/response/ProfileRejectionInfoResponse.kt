package codel.member.presentation.response

import codel.member.domain.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 프로필 거절 정보 조회 응답
 */
@Schema(description = "프로필 거절 정보 응답")
data class ProfileRejectionInfoResponse(
    @Schema(description = "현재 회원 상태", example = "REJECT")
    val status: MemberStatus,
    
    @Schema(description = "얼굴 이미지 거절 여부", example = "true")
    val hasFaceImageRejection: Boolean,
    
    @Schema(description = "코드 이미지 거절 여부", example = "false")
    val hasCodeImageRejection: Boolean,
    
    @Schema(description = "거절된 얼굴 이미지 목록")
    val rejectedFaceImages: List<RejectedImageDto>,
    
    @Schema(description = "거절된 코드 이미지 목록")
    val rejectedCodeImages: List<RejectedImageDto>
)

/**
 * 거절된 이미지 정보
 */
@Schema(description = "거절된 이미지 정보")
data class RejectedImageDto(
    @Schema(description = "이미지 ID", example = "123")
    val imageId: Long,
    
    @Schema(description = "이미지 URL", example = "https://example.com/image1.jpg")
    val url: String,
    
    @Schema(description = "이미지 순서 (1부터 시작)", example = "1")
    val order: Int,
    
    @Schema(description = "거절 사유", example = "얼굴이 명확하게 보이지 않습니다")
    val rejectionReason: String
)
