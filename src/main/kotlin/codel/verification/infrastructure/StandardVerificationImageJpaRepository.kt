package codel.verification.infrastructure

import codel.verification.domain.StandardVerificationImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface StandardVerificationImageJpaRepository : JpaRepository<StandardVerificationImage, Long> {

    /**
     * 활성화된 표준 이미지만 조회 (사용자용)
     * 랜덤으로 하나 선택하기 위해 모두 조회
     */
    fun findAllByIsActiveTrue(): List<StandardVerificationImage>

    /**
     * 모든 표준 이미지 조회 (관리자용)
     */
    fun findAll(): List<StandardVerificationImage>
}