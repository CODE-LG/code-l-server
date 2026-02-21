package codel.member.presentation

import codel.auth.business.AuthService
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.WithdrawnRequest
import codel.member.presentation.request.UpdateRepresentativeQuestionRequest
import codel.member.presentation.response.*
import codel.member.presentation.swagger.MemberControllerSwagger
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import org.springframework.data.domain.Page
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import codel.config.RedisMessageRelay
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
class MemberController(
    private val memberService: MemberService,
    private val authService: AuthService,
    private val asyncNotificationService: IAsyncNotificationService,
    private val redisMessageRelay: RedisMessageRelay,
) : MemberControllerSwagger {
    @PostMapping("/v1/member/login")
    override fun loginMember(
        @RequestBody request: MemberLoginRequest,
    ): ResponseEntity<MemberLoginResponse> {
        val member = memberService.loginMember(request.toMember())
        val token = authService.provideToken(member)
        return ResponseEntity
            .ok()
            .header("Authorization", "Bearer $token")
            .body(MemberLoginResponse(member.getIdOrThrow(), member.memberStatus))
    }

    @PostMapping("/v1/member/fcmtoken")
    override fun saveFcmToken(
        @LoginMember member: Member,
        @RequestBody fcmToken: String,
    ): ResponseEntity<Unit> {
        memberService.saveFcmToken(member, fcmToken)
        return ResponseEntity.ok().build()
    }

    @GetMapping("/v1/member/me")
    override fun findMyProfile(
        @LoginMember member: Member,
    ): ResponseEntity<FullProfileResponse> {
        val findMyProfile = memberService.findMyProfile(member)
        return ResponseEntity.ok(findMyProfile)
    }

    @GetMapping("/v1/member/recommend")
    @Deprecated("Use /api/v1/recommendations/daily-code-matching or /api/v1/recommendations/legacy/recommend instead")
    override fun recommendMembers(
        @LoginMember member: Member,
    ): ResponseEntity<MemberRecommendResponse> {
        val members = memberService.recommendMembers(member)
        return ResponseEntity.ok(MemberRecommendResponse.from(members))
    }

    @GetMapping("/v1/member/all")
    @Deprecated("Use /api/v1/recommendations/random or /api/v1/recommendations/legacy/all instead")
    override fun getRecommendMemberAtTenHourCycle(
        @LoginMember member: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "8") size: Int,
    ): ResponseEntity<Page<FullProfileResponse>> {
        val memberPage = memberService.getRandomMembers(member, page, size)

        return ResponseEntity.ok(
            memberPage.map { member ->
                FullProfileResponse.createOpen(member)
            },
        )
    }

    @GetMapping("/v1/members/{id}")
    override fun getMemberProfileDetail(
        @LoginMember me: Member,
        @PathVariable id: Long,
    ): ResponseEntity<MemberProfileDetailResponse> {
        val memberProfileDetail = memberService.findMemberProfile(me, id)

        return ResponseEntity.ok(memberProfileDetail)
    }

    @PostMapping("/v1/member/me")
    override fun withdrawMember(
        @LoginMember member: Member,
        @RequestBody request : WithdrawnRequest
    ): ResponseEntity<Void> {
        // 1. 회원 탈퇴 처리 (Signal, ChatRoom, Member 상태 변경)
        val chatNotifications = memberService.withdrawMember(member, request.reason)

        // 2. WebSocket으로 채팅방 종료 알림 발송
        chatNotifications.forEach { notification ->
            redisMessageRelay.publish(
                "/sub/v1/chatroom/member/${notification.partner.id}",
                notification.partnerChatRoomResponse
            )

            // 채팅방 구독자들에게 시스템 메시지 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/${notification.partnerChatRoomResponse.chatRoomId}",
                notification.chatResponse
            )
            // 상대방에게 채팅방 종료 알림 전송
        }
        
        // 3. 비동기로 Discord 알림 발송
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getIdOrThrow().toString(),
                    title = "${member.getProfileOrThrow().getCodeNameOrThrow()}님이 탈퇴하였습니다.",
                    body = """
                        👩‍💻 탈퇴 회원: ${member.getProfileOrThrow().getCodeNameOrThrow()}
                        🗓 탈퇴 시각: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}
                        📊 탈퇴 사유: ${request.reason.ifBlank { "미입력" }}
                        💬 종료된 채팅방: ${chatNotifications.size}개
                    """.trimIndent(),
                ),
        )
        
        return ResponseEntity.noContent().build()
    }

    // ========== 프로필 수정 ==========

    /**
     * 코드 이미지 수정
     * - Multipart 파일로 새 이미지를 업로드
     * - existingIds로 유지할 이미지 지정 가능
     * - 상태가 PENDING으로 변경되어 재심사 진행 (필요 시)
     */
    @PutMapping("/v1/member/me/profile/code-images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun updateCodeImages(
        @LoginMember member: Member,
        @RequestParam(value = "codeImages", required = false) codeImages: List<MultipartFile>?,
        @RequestParam(value = "existingIds", required = false) existingIds: List<Long>?
    ): ResponseEntity<UpdateCodeImagesResponse> {
        val response = memberService.updateCodeImages(member, codeImages, existingIds)
        return ResponseEntity.ok(response)
    }

    /**
     * 대표 질문 및 답변 수정
     * - 기존 질문 ID와 새로운 답변으로 수정
     */
    @PutMapping("/v1/member/me/profile/representative-question")
    override fun updateRepresentativeQuestion(
        @LoginMember member: Member,
        @RequestBody request: UpdateRepresentativeQuestionRequest
    ): ResponseEntity<UpdateRepresentativeQuestionResponse> {
        val response = memberService.updateRepresentativeQuestion(
            member,
            request.representativeQuestionId,
            request.representativeAnswer
        )
        return ResponseEntity.ok(response)
    }
}
