package codel.verification.presentation.response

import codel.verification.domain.StandardVerificationImage

/**
 * 표준 인증 이미지 응답 DTO
 */
data class StandardVerificationImageResponse(
    val id: Long,
    val imageUrl: String,
    val description: String
) {
    companion object {
        fun from(standardImage: StandardVerificationImage): StandardVerificationImageResponse {
            return StandardVerificationImageResponse(
                id = standardImage.id!!,
                imageUrl = standardImage.imageUrl,
                description = standardImage.description
            )
        }
    }
}
