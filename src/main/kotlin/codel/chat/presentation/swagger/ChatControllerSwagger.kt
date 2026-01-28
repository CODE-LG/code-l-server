package codel.chat.presentation.swagger

import codel.chat.presentation.request.CreateChatRoomRequest
import codel.chat.presentation.request.ChatLogRequest
import codel.chat.presentation.request.QuestionRecommendRequest
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
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Chat", description = "채팅 관련 API")
interface ChatControllerSwagger {
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
        summary = "채팅방 이전 메시지 조회",
        description = """
            채팅방의 채팅 메시지 목록을 페이징하여 조회합니다.
            
            **파라미터 설명:**
            - chatRoomId: 조회할 채팅방 ID (필수)
            - lastChatId: 마지막으로 읽은 채팅 ID (선택, 무한스크롤 구현용)
            - page: 페이지 번호 (기본값: 0)
            - size: 페이지 크기 (기본값: 30)
            
            **사용 예시:**
            - 최초 로드: GET /v1/chatroom/123/chats/previous?page=0&size=30
            - 이전 메시지 로드: GET /v1/chatroom/123/chats/previous?lastChatId=456&page=0&size=30
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "성공적으로 채팅 목록 조회"),
            ApiResponse(responseCode = "204", description = "이전 채팅 없음"),
            ApiResponse(responseCode = "403", description = "해당 채팅방에 접근할 권한이 없음"),
            ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun getPreviousChats(
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
        summary = "질문 추천",
        description = """
            채팅방에 질문을 추천합니다.

            **버전 분기 동작:**
            - 1.3.0 이상: 카테고리 기반 질문 추천 (request body의 category 필수)
            - 1.3.0 미만 또는 헤더 없음: 기존 랜덤 질문 추천

            **채팅방 카테고리 (1.3.0+):**
            - VALUES: 가치관 코드 (A/B 그룹 적용)
            - TENSION_UP: 텐션업 코드 (랜덤)
            - IF: 만약에 코드 (A/B 그룹 적용)
            - SECRET: 비밀 코드 (A/B 그룹 적용, 19+)

            **A/B 그룹 정책:**
            - A그룹 질문을 우선 추천하고, 소진되면 B그룹 질문 추천
            - 텐션업 코드는 그룹 구분 없이 랜덤 추천
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "질문 추천 성공 또는 소진"),
            ApiResponse(responseCode = "400", description = "카테고리 미선택 또는 잘못된 카테고리 (1.3.0+)"),
            ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음"),
            ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun sendRandomQuestion(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @Parameter(description = "채팅방 ID", required = true, example = "123")
        @PathVariable chatRoomId: Long,
        @Parameter(description = "앱 버전 (1.3.0 이상에서 카테고리 기반 추천)", required = false, example = "1.3.0")
        @RequestHeader(value = "X-App-Version", required = false) appVersion: String?,
        @RequestBody(required = false) request: QuestionRecommendRequest?
    ): ResponseEntity<Any>

    @Operation(
        summary = "채팅방 대화 종료",
        description = "지정된 채팅방의 대화를 종료합니다. 요청한 사용자가 해당 채팅방의 참여자여야 합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "대화 종료 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun closeConversationAtChatRoom(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @Parameter(
            description = "종료할 채팅방의 고유 식별자",
            required = true,
            example = "12345"
        )
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit>

    @Operation(
        summary = "채팅방 나가기",
        description = "지정된 채팅방을 나갑니다. 요청한 사용자가 해당 채팅방의 참여자여야 합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "대화 종료 성공"),
            ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음"),
            ApiResponse(responseCode = "404", description = "존재하지 않는 채팅방"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류")
        ]
    )
    fun leaveChatRoom(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @PathVariable chatRoomId: Long,
    ): ResponseEntity<Unit>
}
