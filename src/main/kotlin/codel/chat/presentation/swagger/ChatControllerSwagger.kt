package codel.chat.presentation.swagger

import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.response.ChatResponse
import codel.chat.presentation.response.ChatRoomResponse
import codel.question.presentation.response.QuestionResponse
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

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
        @Parameter(hidden = true) @LoginMember requester: Member,
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
        @Parameter(hidden = true) @LoginMember requester: Member,
        @PageableDefault(size = 10, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatRoomResponse>>

    @Operation(
        summary = "채팅 목록 조회",
        description = """
            채팅방의 채팅 메시지 목록을 페이징하여 조회합니다.
            
            **파라미터 설명:**
            - chatRoomId: 조회할 채팅방 ID (필수)
            - lastChatId: 마지막으로 읽은 채팅 ID (선택, 무한스크롤 구현용)
            - page: 페이지 번호 (기본값: 0)
            - size: 페이지 크기 (기본값: 30)
            
            **사용 예시:**
            - 최초 로드: GET /v1/chatroom/123/chats?page=0&size=30
            - 이전 메시지 로드: GET /v1/chatroom/123/chats?lastChatId=456&page=0&size=30
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅 목록 조회"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없음"),
            ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getChats(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @Parameter(description = "채팅방 ID", required = true, example = "123")
        @PathVariable chatRoomId: Long,
        @Parameter(description = "마지막으로 읽은 채팅 ID (무한스크롤용)", required = false, example = "456")
        @RequestParam(required = false) lastReadChatId: Long?,
        @PageableDefault(size = 30, page = 0) pageable: Pageable,
    ): ResponseEntity<Page<ChatResponse>>

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
        @Parameter(hidden = true) @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
        @RequestBody chatLogRequest: ChatLogRequest,
    ): ResponseEntity<Unit>

    @Operation(
        summary = "채팅방 코드해제 요청",
        description = "채팅방 코드해제를 요청합니다.."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "성공적으로 코드 해제 요청 성공"),
            ApiResponse(responseCode = "400", description = "요청 값이 잘못됨"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ]
    )
    @PostMapping("/v1/chatroom/{chatRoomId}/unlock")
    fun updateChatRoomStatus(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit>

    @Operation(
        summary = "랜덤 질문 즉시 전송",
        description = "채팅방에 랜덤 질문을 시스템 메시지로 즉시 전송합니다. 버튼 클릭 시 바로 실행됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 랜덤 질문 전송"),
            ApiResponse(responseCode = "204", description = "더 이상 사용할 수 있는 질문이 없음"),
            ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없음"),
            ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun sendRandomQuestion(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @PathVariable chatRoomId: Long
    ): ResponseEntity<ChatResponse>
}
