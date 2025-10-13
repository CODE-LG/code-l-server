package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.ProfileReviewService
import codel.member.domain.Member
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.response.ProfileImagesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "프로필 심사 관리", description = "프로필 심사 거절 및 이미지 교체 API")
@RestController
@RequestMapping("/v1/profile/review")
class ProfileReviewController(
    private val profileReviewService: ProfileReviewService
) {
    
    /**
     * 프로필 거절 정보 조회
     */
    @Operation(summary = "거절 사유 조회", description = "프로필이 거절된 경우 어떤 이미지가 거절되었는지 조회합니다")
    @GetMapping("/rejection-info")
    fun getRejectionInfo(
        @LoginMember member: Member
    ): ResponseEntity<ProfileRejectionInfoResponse> {
        val rejectionInfo = profileReviewService.getRejectionInfo(member)
        return ResponseEntity.ok(rejectionInfo)
    }
    
    /**
     * 프로필 이미지 전체 조회
     */
    @Operation(
        summary = "프로필 이미지 조회",
        description = "프로필의 모든 이미지(얼굴, 코드)를 조회합니다. 거절된 이미지와 승인된 이미지를 모두 포함합니다."
    )
    @GetMapping("/images")
    fun getProfileImages(
        @LoginMember member: Member
    ): ResponseEntity<ProfileImagesResponse> {
        val images = profileReviewService.getProfileImages(member)
        return ResponseEntity.ok(images)
    }
    
    /**
     * 거절된 이미지 전체 교체 (얼굴 + 코드 통합)
     */
    @Operation(
        summary = "거절된 이미지 교체",
        description = """
            거절된 이미지를 새로운 이미지로 교체합니다.
            - 얼굴 이미지: 정확히 2개
            - 코드 이미지: 1~3개
            - 거절된 이미지 타입만 교체 가능
            - 둘 다 거절된 경우 한 번에 모두 교체 가능
        """
    )
    @PutMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replaceImages(
        @LoginMember member: Member,
        @RequestPart(value = "faceImages", required = false) faceImages: List<MultipartFile>?,
        @RequestPart(value = "codeImages", required = false) codeImages: List<MultipartFile>?
    ): ResponseEntity<ReplaceImagesResponse> {
        val response = profileReviewService.replaceImages(member, faceImages, codeImages)
        return ResponseEntity.ok(response)
    }
}
