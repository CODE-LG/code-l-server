package codel.verification.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 사용자 인증 이미지
 *
 * 사용자가 표준 이미지를 보고 동일한 자세로 촬영한 본인 인증 사진
 * - 재제출 가능 (이력 관리)
 * - 소프트 딜리트 지원
 */
@Entity
@Table(
    name = "verification_images",
    indexes = [
        Index(name = "idx_member_id", columnList = "member_id"),
        Index(name = "idx_standard_image_id", columnList = "standard_verification_image_id"),
        Index(name = "idx_deleted_at", columnList = "deleted_at")
    ]
)
class VerificationImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 회원
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: Member,

    /**
     * 참조한 표준 인증 이미지
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "standard_verification_image_id", nullable = false)
    var standardVerificationImage: StandardVerificationImage,

    /**
     * 사용자가 촬영한 인증 이미지 S3 URL
     * 경로: /verification_images/{memberId}/{uuid}.jpg
     */
    @Column(nullable = false, length = 1000)
    var userImageUrl: String,

    /**
     * 소프트 딜리트 시간
     * null이 아니면 삭제된 것으로 간주
     */
    var deletedAt: LocalDateTime? = null

) : BaseTimeEntity() {

    /**
     * 삭제 여부 확인
     */
    fun isDeleted(): Boolean = deletedAt != null

    /**
     * 소프트 딜리트 처리
     */
    fun softDelete() {
        deletedAt = LocalDateTime.now()
    }

    /**
     * 소프트 딜리트 복구
     */
    fun restore() {
        deletedAt = null
    }
}