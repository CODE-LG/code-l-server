package codel.member.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.ProfileReviewService
import codel.member.domain.Member
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.response.ResubmitProfileResponse
import codel.member.presentation.swagger.ProfileReviewControllerSwagger
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/v1/profile/review")
class ProfileReviewController(
    private val profileReviewService: ProfileReviewService,
    private val asyncNotificationService: IAsyncNotificationService
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

        // 비동기로 알림 전송
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "${member.getProfileOrThrow().getCodeNameOrThrow()}님이 재심사를 요청하였습니다.",
                    body = "code:L 프로필 재심사 요청이 왔습니다.",
                ),
        )
        return ResponseEntity.ok(response)
    }

    @PostMapping("/resubmit", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun resubmitProfile(
        @LoginMember member: Member,
        @RequestPart(value = "faceImages", required = false) faceImages: List<MultipartFile>?,
        @RequestPart(value = "codeImages", required = false) codeImages: List<MultipartFile>?,
        @RequestParam(value = "existingFaceImageIds", required = false) existingFaceImageIds: List<Long>?,
        @RequestParam(value = "existingCodeImageIds", required = false) existingCodeImageIds: List<Long>?,
        @RequestParam(value = "standardImageId") standardImageId: Long,
        @RequestPart(value = "verificationImage") verificationImage: MultipartFile
    ): ResponseEntity<ResubmitProfileResponse> {
        val response = profileReviewService.resubmitProfileForReview(
            member = member,
            faceImages = faceImages,
            codeImages = codeImages,
            existingFaceImageIds = existingFaceImageIds,
            existingCodeImageIds = existingCodeImageIds,
            standardImageId = standardImageId,
            verificationImage = verificationImage
        )

        // 비동기로 알림 전송
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "${member.getProfileOrThrow().getCodeNameOrThrow()}님이 재심사를 요청하였습니다.",
                    body = "code:L 프로필 재심사 요청이 왔습니다.",
                ),
        )
        return ResponseEntity.ok(response)
    }
}
