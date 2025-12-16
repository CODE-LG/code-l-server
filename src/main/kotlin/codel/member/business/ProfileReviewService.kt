package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.exception.MemberException
import codel.member.infrastructure.CodeImageRepository
import codel.member.infrastructure.FaceImageRepository
import codel.member.infrastructure.MemberJpaRepository
import codel.member.domain.ImageType
import codel.member.infrastructure.RejectionHistoryRepository
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.RejectedImageDto
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ProfileImageDto
import codel.member.presentation.response.ResubmitProfileResponse
import codel.verification.domain.VerificationImage
import codel.verification.infrastructure.VerificationImageJpaRepository
import codel.verification.infrastructure.StandardVerificationImageJpaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

/**
 * 프로필 심사 관련 서비스
 */
@Service
@Transactional
class ProfileReviewService(
    private val memberJpaRepository: MemberJpaRepository,
    private val faceImageRepository: FaceImageRepository,
    private val codeImageRepository: CodeImageRepository,
    private val imageUploader: ImageUploader,
    private val rejectionHistoryRepository: RejectionHistoryRepository,
    private val verificationImageRepository: VerificationImageJpaRepository,
    private val standardVerificationImageRepository: StandardVerificationImageJpaRepository
) {
    
    /**
     * 프로필 거절 정보 조회
     */
    @Transactional(readOnly = true)
    fun getRejectionInfo(member: Member): ProfileRejectionInfoResponse {
        val profile = member.getProfileOrThrow()
        
        // 거절된 얼굴 이미지 조회
        val rejectedFaceImages = faceImageRepository
            .findByProfileIdAndIsApprovedFalse(profile.id!!)
            .map { image ->
                RejectedImageDto(
                    imageId = image.id,
                    url = image.url,
                    order = image.orders,
                    rejectionReason = image.rejectionReason ?: "사유 없음"
                )
            }
        
        // 거절된 코드 이미지 조회
        val rejectedCodeImages = codeImageRepository
            .findByProfileIdAndIsApprovedFalse(profile.id!!)
            .map { image ->
                RejectedImageDto(
                    imageId = image.id,
                    url = image.url,
                    order = image.orders,
                    rejectionReason = image.rejectionReason ?: "사유 없음"
                )
            }
        
        return ProfileRejectionInfoResponse(
            status = member.memberStatus,
            hasFaceImageRejection = rejectedFaceImages.isNotEmpty(),
            hasCodeImageRejection = rejectedCodeImages.isNotEmpty(),
            rejectedFaceImages = rejectedFaceImages,
            rejectedCodeImages = rejectedCodeImages
        )
    }
    
    /**
     * 프로필 이미지 전체 조회 (승인된 이미지 + 거절된 이미지)
     * - 거절된 이미지가 하나라도 있으면 해당 타입은 빈 리스트 반환
     */
    @Transactional(readOnly = true)
    fun getProfileImages(member: Member): ProfileImagesResponse {
        val profile = member.getProfileOrThrow()
        
        // 얼굴 이미지 조회
        val allFaceImages = faceImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)

        val faceImages = if (allFaceImages.any { !it.isApproved }) {
            // 거절된 이미지가 하나라도 있으면 빈 리스트 반환
            emptyList()
        } else {
            // 모두 승인된 경우에만 이미지 반환
            allFaceImages.map { image ->
                ProfileImageDto(
                    imageId = image.id,
                    url = image.url,
                    order = image.orders,
                    isApproved = image.isApproved,
                    rejectionReason = image.rejectionReason
                )
            }
        }
        
        // 코드 이미지 조회
        val allCodeImages = codeImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)
        val codeImages = if (allCodeImages.any { !it.isApproved }) {
            // 거절된 이미지가 하나라도 있으면 빈 리스트 반환
            emptyList()
        } else {
            // 모두 승인된 경우에만 이미지 반환
            allCodeImages.map { image ->
                ProfileImageDto(
                    imageId = image.id,
                    url = image.url,
                    order = image.orders,
                    isApproved = image.isApproved,
                    rejectionReason = image.rejectionReason
                )
            }
        }
        
        return ProfileImagesResponse(
            faceImages = faceImages,
            codeImages = codeImages
        )
    }
    
    /**
     * 거절된 이미지 전체 교체 (얼굴 + 코드 통합)
     * - existingIds를 통해 유지할 이미지 지정
     * - 지정되지 않은 기존 이미지는 삭제하고 새 이미지로 대체
     */
    fun replaceImages(
        member: Member,
        faceImages: List<MultipartFile>?,
        codeImages: List<MultipartFile>?,
        existingFaceImageIds: List<Long>?,
        existingCodeImageIds: List<Long>?
    ): ReplaceImagesResponse {
        val findMember = memberJpaRepository.findMemberWithProfile(member.getIdOrThrow())
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "회원을 조회할 수 없습니다.")
        val profile = findMember.getProfileOrThrow()

        var uploadedCount = 0
        val messages = mutableListOf<String>()
        var changed = false

        // --- 얼굴 이미지 섹션: 입력이 있을 때만 처리 ---
        if (!faceImages.isNullOrEmpty() || !existingFaceImageIds.isNullOrEmpty()) {
            val keepIds = (existingFaceImageIds ?: emptyList()).toSet()

            // 유지할 기존(정확히 keepIds에 포함된 것)
            val kept = profile.faceImages
                .sortedBy { it.orders }
                .filter { it.id != null && it.id in keepIds }

            val newCount = faceImages?.size ?: 0
            val total = kept.size + newCount
            if (total != 2) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "얼굴 이미지는 총 2개여야 합니다. (현재: 유지 ${kept.size}개 + 신규 ${newCount}개 = ${total}개)"
                )
            }

            // 제거(컬렉션에서만 조작 → orphanRemoval로 DB 삭제, 역참조도 해제)
            profile.faceImages.filter { it !in kept }.toList().forEach { img ->
                profile.faceImages.remove(img)
                // S3 삭제는 AfterCommit 이벤트에서 권장
            }

            // 신규 추가
            val added = faceImages.orEmpty().mapIndexed { idx, file ->
                val url = imageUploader.uploadFile(file)
                FaceImage(profile = profile, url = url, orders = kept.size + idx, isApproved = true)
            }
            added.forEach { img ->
                profile.faceImages.add(img)
            }

            // order 재정렬(0..n-1)
            profile.faceImages.sortedBy { it.orders }.forEachIndexed { i, img -> img.orders = i }

            // Dual write
            profile.updateFaceImageUrls(profile.faceImages.sortedBy { it.orders }.map { it.url })

            uploadedCount += newCount
            messages += "얼굴 이미지 2개 반영(유지 ${kept.size}, 신규 ${newCount})"
            changed = true
        }

        // --- 코드 이미지 섹션: 입력이 있을 때만 처리 ---
        if (!codeImages.isNullOrEmpty() || !existingCodeImageIds.isNullOrEmpty()) {
            val keepIds = (existingCodeImageIds ?: emptyList()).toSet()

            val kept = profile.codeImages
                .sortedBy { it.orders }
                .filter { it.id != null && it.id in keepIds }

            val newCount = codeImages?.size ?: 0
            val total = kept.size + newCount
            if (total !in 1..3) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "코드 이미지는 1~3개여야 합니다. (현재: 유지 ${kept.size}개 + 신규 ${newCount}개 = ${total}개)"
                )
            }

            profile.codeImages.filter { it !in kept }.toList().forEach { img ->
                profile.codeImages.remove(img)
            }

            val added = codeImages.orEmpty().mapIndexed { idx, file ->
                val url = imageUploader.uploadFile(file)
                CodeImage(profile = profile, url = url, orders = kept.size + idx, isApproved = true)
            }
            added.forEach { img ->
                profile.codeImages.add(img)
            }

            profile.codeImages.sortedBy { it.orders }.forEachIndexed { i, img -> img.orders = i }

            profile.updateCodeImageUrls(profile.codeImages.sortedBy { it.orders }.map { it.url })

            uploadedCount += newCount
            messages += "코드 이미지 ${total}개 반영(유지 ${kept.size}, 신규 ${newCount})"
            changed = true
        }

        // 변경이 있었으면 재심사(정책에 맞게 조정)
        if (changed) {
            findMember.memberStatus = MemberStatus.HIDDEN_COMPLETED
            messages += "심사가 다시 진행됩니다"
        } else {
            messages += "변경된 이미지가 없습니다"
        }

        // 영속 + @Transactional → save 호출 불필요 (merge 유발 금지)
        return ReplaceImagesResponse(
            uploadedCount = uploadedCount,
            profileStatus = findMember.memberStatus,
            message = messages.joinToString(". ")
        )
    }

    /**
     * 재심사 요청 (코드/얼굴/인증 이미지 통합 제출)
     * - 2번 과정(코드/얼굴 이미지)과 3번 과정(인증 이미지)을 통합
     * - 모든 이미지를 한 번에 제출하여 PENDING 상태로 변경
     *
     * @param member 회원
     * @param faceImages 얼굴 이미지 (신규 업로드)
     * @param codeImages 코드 이미지 (신규 업로드)
     * @param existingFaceImageIds 유지할 기존 얼굴 이미지 ID 목록
     * @param existingCodeImageIds 유지할 기존 코드 이미지 ID 목록
     * @param standardImageId 표준 인증 이미지 ID
     * @param verificationImage 본인 인증 이미지
     */
    fun resubmitProfileForReview(
        member: Member,
        faceImages: List<MultipartFile>?,
        codeImages: List<MultipartFile>?,
        existingFaceImageIds: List<Long>?,
        existingCodeImageIds: List<Long>?,
        standardImageId: Long,
        verificationImage: MultipartFile
    ): ResubmitProfileResponse {
        // 1. 회원 상태 검증: REJECT 상태여야 함
        require(member.memberStatus == MemberStatus.REJECT) {
            "재심사 요청은 REJECT 상태에서만 가능합니다. 현재 상태: ${member.memberStatus}"
        }

        val findMember = memberJpaRepository.findMemberWithProfile(member.getIdOrThrow())
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "회원을 조회할 수 없습니다.")
        val profile = findMember.getProfileOrThrow()

        val messages = mutableListOf<String>()

        // 2. 얼굴 이미지 처리
        if (!faceImages.isNullOrEmpty() || !existingFaceImageIds.isNullOrEmpty()) {
            val keepIds = (existingFaceImageIds ?: emptyList()).toSet()
            val kept = profile.faceImages
                .sortedBy { it.orders }
                .filter { it.id != null && it.id in keepIds }

            val newCount = faceImages?.size ?: 0
            val total = kept.size + newCount
            if (total != 2) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "얼굴 이미지는 총 2개여야 합니다. (현재: 유지 ${kept.size}개 + 신규 ${newCount}개 = ${total}개)"
                )
            }

            profile.faceImages.filter { it !in kept }.toList().forEach { img ->
                profile.faceImages.remove(img)
            }

            val added = faceImages.orEmpty().mapIndexed { idx, file ->
                val url = imageUploader.uploadFile(file)
                FaceImage(profile = profile, url = url, orders = kept.size + idx, isApproved = true)
            }
            added.forEach { img ->
                profile.faceImages.add(img)
            }

            profile.faceImages.sortedBy { it.orders }.forEachIndexed { i, img -> img.orders = i }
            profile.updateFaceImageUrls(profile.faceImages.sortedBy { it.orders }.map { it.url })

            messages += "얼굴 이미지 2개 반영"
        }

        // 3. 코드 이미지 처리
        if (!codeImages.isNullOrEmpty() || !existingCodeImageIds.isNullOrEmpty()) {
            val keepIds = (existingCodeImageIds ?: emptyList()).toSet()
            val kept = profile.codeImages
                .sortedBy { it.orders }
                .filter { it.id != null && it.id in keepIds }

            val newCount = codeImages?.size ?: 0
            val total = kept.size + newCount
            if (total !in 1..3) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "코드 이미지는 1~3개여야 합니다. (현재: 유지 ${kept.size}개 + 신규 ${newCount}개 = ${total}개)"
                )
            }

            profile.codeImages.filter { it !in kept }.toList().forEach { img ->
                profile.codeImages.remove(img)
            }

            val added = codeImages.orEmpty().mapIndexed { idx, file ->
                val url = imageUploader.uploadFile(file)
                CodeImage(profile = profile, url = url, orders = kept.size + idx, isApproved = true)
            }
            added.forEach { img ->
                profile.codeImages.add(img)
            }

            profile.codeImages.sortedBy { it.orders }.forEachIndexed { i, img -> img.orders = i }
            profile.updateCodeImageUrls(profile.codeImages.sortedBy { it.orders }.map { it.url })

            messages += "코드 이미지 ${total}개 반영"
        }

        // 4. 본인 인증 이미지 처리
        // 기존 인증 이미지가 있으면 소프트 딜리트
        val existingVerificationImage = verificationImageRepository
            .findFirstByMemberAndDeletedAtIsNullOrderByCreatedAtDesc(findMember)
        existingVerificationImage?.softDelete()

        // 표준 이미지 조회
        val standardImage = standardVerificationImageRepository.findById(standardImageId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "표준 인증 이미지를 찾을 수 없습니다. ID: $standardImageId")
        }

        // S3에 인증 이미지 업로드
        val verificationImageUrl = imageUploader.uploadFile(verificationImage)

        // VerificationImage 엔티티 생성 및 저장
        val newVerificationImage = VerificationImage(
            member = findMember,
            standardVerificationImage = standardImage,
            userImageUrl = verificationImageUrl
        )
        verificationImageRepository.save(newVerificationImage)

        messages += "본인 인증 이미지 제출 완료"

        // 5. Member 상태를 HIDDEN_COMPLETED으로 변경
        findMember.memberStatus = MemberStatus.PENDING
        messages += "심사 대기 상태로 변경되었습니다"

        return ResubmitProfileResponse(
            status = findMember.memberStatus,
            message = messages.joinToString(". "),
        )
    }
}
