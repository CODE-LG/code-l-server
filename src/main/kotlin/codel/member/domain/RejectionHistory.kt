package codel.member.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * 프로필 심사 거절 이력 엔티티
 * - 회원의 프로필이 거절될 때마다 차수를 증가시키며 이력을 보관
 * - S3 이미지 URL을 보존하여 과거 거절 내역 조회 가능
 */
@Entity
@Table(name = "rejection_histories")
class RejectionHistory(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member,

    @Column(name = "rejection_round", nullable = false)
    val rejectionRound: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 50)
    val imageType: ImageType,

    @Column(name = "image_id", nullable = false)
    val imageId: Long,

    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,

    @Column(name = "image_order", nullable = false)
    val imageOrder: Int,

    @Column(nullable = false, length = 1000)
    val reason: String,

    @Column(name = "rejected_at", nullable = false)
    val rejectedAt: LocalDateTime
) : BaseTimeEntity() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RejectionHistory) return false
        if (id == 0L) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String {
        return "RejectionHistory(id=$id, memberId=${member.id}, round=$rejectionRound, imageType=$imageType)"
    }
}

/**
 * 거절된 이미지의 타입
 */
enum class ImageType {
    FACE_IMAGE,
    CODE_IMAGE
}
