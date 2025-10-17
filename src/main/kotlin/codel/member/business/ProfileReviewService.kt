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
        
        // 상태 검증
        if (findMember.memberStatus != MemberStatus.REJECT) {
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
        if (hasRejectedFaceImages) {
            val existingFaceImages = faceImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)
            val keepIds = existingFaceImageIds?.toSet() ?: emptySet()
            
            // 유지할 이미지 개수
            val keptImages = existingFaceImages.filter { it.id in keepIds }
            
            // 새로 업로드할 이미지 개수
            val newImageCount = (faceImages?.size ?: 0)
            
            // 최종 이미지 개수 (유지 + 신규) = 2개여야 함
            val totalCount = keptImages.size + newImageCount
            if (totalCount != 2) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "얼굴 이미지는 총 2개여야 합니다. (현재: 유지 ${keptImages.size}개 + 신규 ${newImageCount}개 = ${totalCount}개)"
                )
            }
            
            // 유지하지 않을 이미지 삭제
            val imagesToDelete = existingFaceImages.filter { it.id !in keepIds }
            faceImageRepository.deleteAll(imagesToDelete)
            // TODO: S3에서 파일도 삭제 필요
            
            // 새 이미지 업로드 및 엔티티 생성
            val newFaceImages = faceImages?.mapIndexed { index, file ->
                val url = imageUploader.uploadFile(file)
                codel.member.domain.FaceImage(
                    profile = profile,
                    url = url,
                    orders = keptImages.size + index,
                    isApproved = true
                )
            } ?: emptyList()
            
            // 새 이미지 엔티티 저장
            faceImageRepository.saveAll(newFaceImages)
            
            // Profile의 String 필드만 업데이트 (Dual Write)
            val allFaceImageUrls = keptImages.map { it.url } + newFaceImages.map { it.url }
            profile.updateFaceImageUrls(allFaceImageUrls)
            
            uploadedCount += newImageCount
            messages.add("얼굴 이미지 ${totalCount}개 (유지: ${keptImages.size}개, 신규: ${newImageCount}개)")
        }
        
        // 코드 이미지 교체
        if (hasRejectedCodeImages) {
            val existingCodeImages = codeImageRepository.findByProfileIdOrderByOrdersAsc(profile.id!!)
            val keepIds = existingCodeImageIds?.toSet() ?: emptySet()
            
            // 유지할 이미지 개수
            val keptImages = existingCodeImages.filter { it.id in keepIds }
            
            // 새로 업로드할 이미지 개수
            val newImageCount = (codeImages?.size ?: 0)
            
            // 최종 이미지 개수 (유지 + 신규) = 1~3개여야 함
            val totalCount = keptImages.size + newImageCount
            if (totalCount !in 1..3) {
                throw MemberException(
                    HttpStatus.BAD_REQUEST,
                    "코드 이미지는 1~3개여야 합니다. (현재: 유지 ${keptImages.size}개 + 신규 ${newImageCount}개 = ${totalCount}개)"
                )
            }
            
            // 유지하지 않을 이미지 삭제
            val imagesToDelete = existingCodeImages.filter { it.id !in keepIds }
            codeImageRepository.deleteAll(imagesToDelete)
            // TODO: S3에서 파일도 삭제 필요
            
            // 새 이미지 업로드 및 엔티티 생성
            val newCodeImages = codeImages?.mapIndexed { index, file ->
                val url = imageUploader.uploadFile(file)
                codel.member.domain.CodeImage(
                    profile = profile,
                    url = url,
                    orders = keptImages.size + index,
                    isApproved = true
                )
            } ?: emptyList()
            
            // 새 이미지 엔티티 저장
            codeImageRepository.saveAll(newCodeImages)
            
            // Profile의 String 필드만 업데이트 (Dual Write)
            val allCodeImageUrls = keptImages.map { it.url } + newCodeImages.map { it.url }
            profile.updateCodeImageUrls(allCodeImageUrls)
            
            uploadedCount += newImageCount
            messages.add("코드 이미지 ${totalCount}개 (유지: ${keptImages.size}개, 신규: ${newImageCount}개)")
        }

        findMember.memberStatus = MemberStatus.PENDING
        messages.add("심사가 다시 진행됩니다")
        
        memberJpaRepository.save(findMember)
        
        return ReplaceImagesResponse(
            uploadedCount = uploadedCount,
            profileStatus = findMember.memberStatus,
            message = messages.joinToString(". ")
        )
    }
}
