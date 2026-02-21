package codel.block.presentation

import codel.block.business.BlockService
import codel.block.presentation.request.BlockMemberRequest
import codel.block.presentation.swagger.BlockControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.business.MemberService
import codel.member.domain.Member
import codel.notification.business.IAsyncNotificationService
import codel.notification.domain.Notification
import codel.config.RedisMessageRelay
import codel.notification.domain.NotificationType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/v1/block")
class BlockController(
    val blockService: BlockService,
    val memberService : MemberService,
    val redisMessageRelay: RedisMessageRelay,
    val asyncNotificationService: IAsyncNotificationService,
) : BlockControllerSwagger {

    @PostMapping
    override fun blockMember(
        @LoginMember blocker: Member,
        @RequestBody blockMemberRequest: BlockMemberRequest
    ): ResponseEntity<Unit> {
        val savedChatDto = blockService.blockMember(blocker, blockMemberRequest.blockedMemberId)

        // 채팅방이 있었고 시스템 메시지가 생성된 경우에만 WebSocket 전송
        savedChatDto?.let { responseDto ->
            // 상대방에게는 읽지 않은 수가 증가된 채팅방 정보 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/member/${responseDto.partner.id}",
                responseDto.partnerChatRoomResponse,
            )

            // 발송자에게는 본인 기준 채팅방 정보 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/member/${blocker.id}",
                responseDto.requesterChatRoomResponse,
            )

            // 채팅방 구독자들에게 실시간 메시지 전송
            redisMessageRelay.publish(
                "/sub/v1/chatroom/${responseDto.requesterChatRoomResponse.chatRoomId}",
                responseDto.chatResponse
            )
        }

        // 디스코드 알림은 채팅방 존재 여부와 관계없이 항상 전송
        val blockedMember = memberService.findMember(blockMemberRequest.blockedMemberId)
        asyncNotificationService.sendAsync(
            notification =
                Notification(
                    type = NotificationType.DISCORD,
                    targetId = blocker.getProfileOrThrow().toString(),
                    title = "🚨 차단 접수 알림",
                    body = buildString {
                        append("👮‍♀️ 차단자: ${blocker.getProfileOrThrow().getCodeNameOrThrow()}\n")
                        append("🎯 피차단자: ${blockedMember.getProfileOrThrow().getCodeNameOrThrow()}\n")
                        append("🗓 차단 시각: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}")
                    },
                ),
        )

        return ResponseEntity.ok().build()
    }

    @DeleteMapping("/{memberId}")
    override fun unBlockMember(
        @LoginMember blocker: Member,
        @PathVariable memberId: Long
    ): ResponseEntity<Unit> {
        blockService.unBlockMember(blocker, memberId)
        return ResponseEntity.noContent().build()
    }
}