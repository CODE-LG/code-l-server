package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.ProfileReviewService
import codel.member.domain.Member
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.swagger.ProfileReviewControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
class ProfileReviewController(
    private val profileReviewService: ProfileReviewService
) : ProfileReviewControllerSwagger {

    override fun getRejectionInfo(
        @LoginMember member: Member
    ): ResponseEntity<ProfileRejectionInfoResponse> {
        val rejectionInfo = profileReviewService.getRejectionInfo(member)
        return ResponseEntity.ok(rejectionInfo)
    }

    override fun getProfileImages(
        @LoginMember member: Member
    ): ResponseEntity<ProfileImagesResponse> {
        val images = profileReviewService.getProfileImages(member)
        return ResponseEntity.ok(images)
    }

    override fun replaceImages(
        @LoginMember member: Member,
        faceImages: List<MultipartFile>?,
        codeImages: List<MultipartFile>?
    ): ResponseEntity<ReplaceImagesResponse> {
        val response = profileReviewService.replaceImages(member, faceImages, codeImages)
        return ResponseEntity.ok(response)
    }
}
