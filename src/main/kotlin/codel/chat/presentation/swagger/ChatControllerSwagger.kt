package codel.chat.presentation.swagger

import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.UpdateLastChatRequest
import codel.chat.presentation.response.ChatResponses
import codel.chat.presentation.response.ChatRoomResponse
import codel.chat.presentation.response.ChatRoomResponses
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Chat", description = "채팅 관련 API")
interface ChatControllerSwagger {
    @Operation(
        summary = "매칭 후 채팅방 생성 (매칭 기능과 연계되면 삭제 예정)",
        description = "매칭이 성사되면 채팅방을 생성하고 상대방의 채팅방 목록에 채팅방 생성",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅방 생성"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun createChatRoom(
        @LoginMember requester: Member,
        @RequestBody request: CreateChatRoomRequest,
    ): ResponseEntity<ChatRoomResponse>

    @Operation(
        summary = "채팅방 목록 조회",
        description = "내가 참여하고 있는 채팅방 목록 조회",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅방 목록 조회"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getChatRooms(
        @LoginMember requester: Member,
    ): ResponseEntity<ChatRoomResponses>

    @Operation(
        summary = "채팅 목록 조회",
        description = "내가 참여하고 있는 채팅방의 모든 채팅 목록 조회",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅 목록 조회"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getChats(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<ChatResponses>

    @Operation(
        summary = "채팅방에서 마지막으로 읽은 채팅 업데이트",
        description = "채팅방을 나가면서 이 채팅방에서 내가 읽은 마지막 채팅 정보 저장",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공적으로 마지막 채팅 정보 저장"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun updateLastChat(
        @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody updateLastChatRequest: UpdateLastChatRequest,
    ): ResponseEntity<Unit>
}
