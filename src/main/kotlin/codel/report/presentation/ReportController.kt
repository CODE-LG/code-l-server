package codel.report.presentation

import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import codel.report.business.ReportService
import codel.report.presentation.request.ReportRequest
import codel.report.presentation.swagger.ReportControllerSwagger
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/v1/reports")
class ReportController(
    val reportService: ReportService,
    val memberService : MemberService,
    val asyncNotificationService: IAsyncNotificationService,
    val messagingTemplate: SimpMessagingTemplate,
) : ReportControllerSwagger {

    @PostMapping
    override fun reportMember(
        @LoginMember member: Member,
        @RequestBody reportRequest: ReportRequest
    ): ResponseEntity<Unit> {
        val savedChatDto = reportService.report(member, reportRequest.reportedId, reportRequest.reason)

        // 채팅방이 있었고 시스템 메시지가 생성된 경우에만 WebSocket 전송
        savedChatDto?.let { responseDto ->
            // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${responseDto.partner.id}",
                responseDto.partnerChatRoomResponse,
            )

            // 발송자에게는 본인 기준 채팅방 정보 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${member.id}",
                responseDto.requesterChatRoomResponse,
            )

            // 채팅방 구독자들에게 실시간 메시지 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/${responseDto.requesterChatRoomResponse.chatRoomId}",
                responseDto.chatResponse
            )
        }

        // 디스코드 알림은 채팅방 존재 여부와 관계없이 항상 전송
        val reportedMember = memberService.findMember(reportRequest.reportedId)
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = member.getProfileOrThrow().toString(),
                    title = "🚨 신고 접수 알림",
                    body = buildString {
                        append("👮‍♀️ 신고자: ${member.getProfileOrThrow().getCodeNameOrThrow()}\n")
                        append("🎯 피신고자: ${reportedMember.getProfileOrThrow().getCodeNameOrThrow()}\n")
                        append("🗓 신고 시각: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}\n")
                        append("💬 신고 사유: ${reportRequest.reason.ifBlank { "미입력" }}")
                    },
                ),
        )

        return ResponseEntity.ok().build()
    }
}