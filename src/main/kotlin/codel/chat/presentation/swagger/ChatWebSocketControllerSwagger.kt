package codel.chat.presentation.swagger

import codel.chat.presentation.request.ChatRequest
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.Payload

@Tag(name = "ChatWebSocket", description = "채팅 웹소켓 관련 API")
interface ChatWebSocketControllerSwagger {
    @Operation(
        summary = "채팅 생성 후 전파",
        description = "채팅을 생성하고 상대방이 보고있는 채팅방 목록과 채팅 목록을 업데이트",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅을 보냄"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun sendChat(
        @DestinationVariable("chatRoomId") chatRoomId: Long,
        @LoginMember requester: Member,
        @Payload chatRequest: ChatRequest,
    )
}
