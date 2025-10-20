package codel.block.presentation

import codel.block.business.BlockService
import codel.block.presentation.request.BlockMemberRequest
import codel.block.presentation.swagger.BlockControllerSwagger
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import org.springframework.http.ResponseEntity
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/block")
class BlockController(
    val blockService: BlockService,
    val messagingTemplate: SimpMessagingTemplate
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
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${responseDto.partner.id}",
                responseDto.partnerChatRoomResponse,
            )

            // 발송자에게는 본인 기준 채팅방 정보 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/member/${blocker.id}",
                responseDto.requesterChatRoomResponse,
            )

            // 채팅방 구독자들에게 실시간 메시지 전송
            messagingTemplate.convertAndSend(
                "/sub/v1/chatroom/${responseDto.requesterChatRoomResponse.chatRoomId}",
                responseDto.chatResponse
            )
        }

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