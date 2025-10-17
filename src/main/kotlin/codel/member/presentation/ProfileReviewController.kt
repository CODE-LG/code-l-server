package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.ProfileReviewService
import codel.member.domain.Member
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.swagger.ProfileReviewControllerSwagger
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/profile/review")
class ProfileReviewController(
    private val profileReviewService: ProfileReviewService
) : ProfileReviewControllerSwagger {


    @GetMapping("/rejection-info")
    override fun getRejectionInfo(
        @LoginMember member: Member
    ): ResponseEntity<ProfileRejectionInfoResponse> {
        val rejectionInfo = profileReviewService.getRejectionInfo(member)
        return ResponseEntity.ok(rejectionInfo)
    }


    @GetMapping("/images")
    override fun getProfileImages(
        @LoginMember member: Member
    ): ResponseEntity<ProfileImagesResponse> {
        val images = profileReviewService.getProfileImages(member)
        return ResponseEntity.ok(images)
    }


    @PutMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun replaceImages(
        @LoginMember member: Member,
        faceImages: List<MultipartFile>?,
        codeImages: List<MultipartFile>?,
        existingFaceImageIds: List<Long>?,
        existingCodeImageIds: List<Long>?
    ): ResponseEntity<ReplaceImagesResponse> {
        val response = profileReviewService.replaceImages(
            member, 
            faceImages, 
            codeImages,
            existingFaceImageIds,
            existingCodeImageIds
        )
        return ResponseEntity.ok(response)
    }
}
