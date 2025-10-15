package codel.member.presentation.response

import codel.member.domain.MemberStatus
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 이미지 교체 응답
 */
@Schema(description = "이미지 교체 응답")
data class ReplaceImagesResponse(
    @Schema(description = "업로드된 이미지 개수", example = "3")
    val uploadedCount: Int,
    
    @Schema(description = "업데이트된 프로필 상태", example = "PENDING")
    val profileStatus: MemberStatus,
    
    @Schema(description = "응답 메시지", example = "이미지가 성공적으로 교체되었습니다. 관리자 승인을 기다려주세요.")
    val message: String
)
