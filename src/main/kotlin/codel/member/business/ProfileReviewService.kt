package codel.member.business

import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.exception.MemberException
import codel.member.infrastructure.CodeImageRepository
import codel.member.infrastructure.FaceImageRepository
import codel.member.infrastructure.MemberJpaRepository
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.RejectedImageDto
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ProfileImageDto
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

/**
 * 프로필 심사 관련 서비스
 */
@Service
@Transactional
class ProfileReviewService(
    private val memberJpaRepository: MemberJpaRepository,
    private val faceImageRepository: FaceImageRepository,
    private val codeImageRepository: CodeImageRepository,
    private val imageUploader: ImageUploader
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
     */
    fun replaceImages(
        member: Member,
        faceImages: List<MultipartFile>?,
        codeImages: List<MultipartFile>?
    ): ReplaceImagesResponse {
        val profile = member.getProfileOrThrow()
        
        // 상태 검증
        if (member.memberStatus != MemberStatus.REJECT) {
            throw MemberException(HttpStatus.BAD_REQUEST, "거절 상태가 아닌 프로필은 수정할 수 없습니다")
        }
        
        // 거절된 이미지 확인
        val hasRejectedFaceImages = faceImageRepository
            .findByProfileIdAndIsApprovedFalse(profile.id!!)
            .isNotEmpty()
        
        val hasRejectedCodeImages = codeImageRepository
            .findByProfileIdAndIsApprovedFalse(profile.id!!)
            .isNotEmpty()
        
        // 거절된 이미지가 없으면 에러
        if (!hasRejectedFaceImages && !hasRejectedCodeImages) {
            throw MemberException(HttpStatus.BAD_REQUEST, "거절된 이미지가 없습니다")
        }
        
        var uploadedCount = 0
        val messages = mutableListOf<String>()
        
        // 얼굴 이미지 교체
        if (faceImages != null && faceImages.isNotEmpty()) {
            if (!hasRejectedFaceImages) {
                throw MemberException(HttpStatus.BAD_REQUEST, "거절된 얼굴 이미지가 없습니다")
            }
            
            // 얼굴 이미지 개수 검증 (정확히 2개)
            if (faceImages.size != 2) {
                throw MemberException(HttpStatus.BAD_REQUEST, "얼굴 이미지는 정확히 2개여야 합니다")
            }
            
            // 기존 얼굴 이미지 삭제
            val existingFaceImages = faceImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)
            // TODO: S3에서 파일 삭제 구현 필요
            faceImageRepository.deleteAll(existingFaceImages)
            
            // 새 이미지 업로드
            val newFaceImageUrls = faceImages.map { file ->
                imageUploader.uploadFile(file)
            }
            
            // Profile 업데이트 (Dual Write)
            profile.replaceAllFaceImages(newFaceImageUrls)
            uploadedCount += newFaceImageUrls.size
            messages.add("얼굴 이미지 ${newFaceImageUrls.size}개 업로드 완료")
        }
        
        // 코드 이미지 교체
        if (codeImages != null && codeImages.isNotEmpty()) {
            if (!hasRejectedCodeImages) {
                throw MemberException(HttpStatus.BAD_REQUEST, "거절된 코드 이미지가 없습니다")
            }
            
            // 코드 이미지 개수 검증 (1~3개)
            if (codeImages.size !in 1..3) {
                throw MemberException(HttpStatus.BAD_REQUEST, "코드 이미지는 1개 이상 3개 이하여야 합니다")
            }
            
            // 기존 코드 이미지 삭제
            val existingCodeImages = codeImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)
            // TODO: S3에서 파일 삭제 구현 필요
            codeImageRepository.deleteAll(existingCodeImages)
            
            // 새 이미지 업로드
            val newCodeImageUrls = codeImages.map { file ->
                imageUploader.uploadFile(file)
            }
            
            // Profile 업데이트 (Dual Write)
            profile.replaceAllCodeImages(newCodeImageUrls)
            uploadedCount += newCodeImageUrls.size
            messages.add("코드 이미지 ${newCodeImageUrls.size}개 업로드 완료")
        }
        
        // 모든 거절된 이미지가 교체되었는지 확인
        val stillHasRejectedFaceImages = if (faceImages == null || faceImages.isEmpty()) {
            hasRejectedFaceImages
        } else {
            false
        }
        
        val stillHasRejectedCodeImages = if (codeImages == null || codeImages.isEmpty()) {
            hasRejectedCodeImages
        } else {
            false
        }
        
        // 모든 거절된 이미지가 교체되었으면 PENDING 상태로 변경
        if (!stillHasRejectedFaceImages && !stillHasRejectedCodeImages) {
            member.memberStatus = MemberStatus.PENDING
            messages.add("심사가 다시 진행됩니다")
        } else {
            if (stillHasRejectedFaceImages) {
                messages.add("얼굴 이미지도 교체해주세요")
            }
            if (stillHasRejectedCodeImages) {
                messages.add("코드 이미지도 교체해주세요")
            }
        }
        
        memberJpaRepository.save(member)
        
        return ReplaceImagesResponse(
            uploadedCount = uploadedCount,
            profileStatus = member.memberStatus,
            message = messages.joinToString(". ")
        )
    }
}
