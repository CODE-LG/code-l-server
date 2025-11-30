package codel.verification.domain

import codel.common.domain.BaseTimeEntity
import jakarta.persistence.*

/**
 * 표준 인증 이미지 (관리자가 등록한 가이드 포즈 이미지)
 *
 * 사용자는 이 표준 이미지를 보고 동일한 자세로 인증 이미지를 촬영합니다.
 */
@Entity
@Table(name = "standard_verification_images")
class StandardVerificationImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    /**
     * 표준 이미지 S3 URL
     * 예: https://s3.../standard_verification_images/pose1.jpg
     */
    @Column(nullable = false, length = 1000)
    var imageUrl: String,

    /**
     * 포즈 설명
     * 예: "정면을 보고 양손을 귀 옆에 올려주세요"
     */
    @Column(nullable = false)
    var description: String,

    /**
     * 활성화 여부
     * false인 경우 사용자에게 노출되지 않음
     */
    @Column(nullable = false)
    var isActive: Boolean = true

) : BaseTimeEntity()