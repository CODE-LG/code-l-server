package codel.admin.presentation.response

import codel.member.domain.ImageType
import codel.member.domain.RejectionHistory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 거절 이력 응답 DTO
 */
data class RejectionHistoryResponse(
    val id: Long,
    val rejectionRound: Int,
    val imageType: String,
    val imageUrl: String,
    val imageOrder: Int,
    val reason: String,
    val rejectedAt: String
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        
        fun from(history: RejectionHistory): RejectionHistoryResponse {
            return RejectionHistoryResponse(
                id = history.id,
                rejectionRound = history.rejectionRound,
                imageType = when (history.imageType) {
                    ImageType.FACE_IMAGE -> "얼굴 이미지"
                    ImageType.CODE_IMAGE -> "코드 이미지"
                },
                imageUrl = history.imageUrl,
                imageOrder = history.imageOrder + 1, // 0-based를 1-based로 변환
                reason = history.reason,
                rejectedAt = history.rejectedAt.format(formatter)
            )
        }
    }
}

/**
 * 회원의 전체 거절 이력 응답
 */
data class MemberRejectionHistoriesResponse(
    val memberId: Long,
    val memberName: String,
    val totalRejectionCount: Int,
    val maxRejectionRound: Int,
    val histories: List<RejectionHistoryResponse>
)
