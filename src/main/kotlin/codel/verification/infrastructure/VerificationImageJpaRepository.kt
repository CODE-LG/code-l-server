package codel.verification.infrastructure

import codel.member.domain.Member
import codel.verification.domain.VerificationImage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface VerificationImageJpaRepository : JpaRepository<VerificationImage, Long> {

    /**
     * 회원의 최신 인증 이미지 조회 (삭제되지 않은 것만)
     * 재제출이 가능하므로 가장 최근 것만 조회
     */
    fun findFirstByMemberAndDeletedAtIsNullOrderByCreatedAtDesc(member: Member): VerificationImage?

    /**
     * 회원의 모든 인증 이미지 이력 조회 (관리자용)
     * 삭제된 것 포함, 최신순
     */
    fun findAllByMemberOrderByCreatedAtDesc(member: Member): List<VerificationImage>

    /**
     * 회원의 유효한 인증 이미지 이력 조회
     * 삭제되지 않은 것만, 최신순
     */
    fun findAllByMemberAndDeletedAtIsNullOrderByCreatedAtDesc(member: Member): List<VerificationImage>
}
