package codel.verification.business

import codel.config.Loggable
import codel.verification.infrastructure.StandardVerificationImageJpaRepository
import codel.verification.presentation.response.StandardVerificationImageResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional(readOnly = true)
class VerificationImageService(
    private val standardVerificationImageRepository: StandardVerificationImageJpaRepository
) : Loggable{

    /**
     * 활성화된 표준 인증 이미지 중 랜덤으로 하나 조회 (사용자용)
     * isActive = true인 이미지 중 랜덤 선택
     */
    fun getRandomStandardImage(): StandardVerificationImageResponse {
        log.info { "활성화된 표준 인증 이미지 랜덤 조회" }

        val standardImages = standardVerificationImageRepository.findAllByIsActiveTrue()

        require(standardImages.isNotEmpty()) { "활성화된 표준 인증 이미지가 없습니다." }

        val randomImage = standardImages.random()

        log.info { "랜덤 선택된 표준 이미지 ID: ${randomImage.id}" }

        return StandardVerificationImageResponse.from(randomImage)
    }
}