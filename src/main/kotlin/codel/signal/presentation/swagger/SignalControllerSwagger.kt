package codel.signal.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.response.UnlockedMemberProfileResponse
import codel.signal.presentation.request.HideSignalRequest
import codel.signal.presentation.request.SendSignalRequest
import codel.signal.presentation.response.SignalMemberResponse
import codel.signal.presentation.response.SignalResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import io.swagger.v3.oas.annotations.parameters.RequestBody as SwaggerRequestBody

@Tag(name = "Signal", description = "시그널(신호) 관련 API")
interface SignalControllerSwagger {
    @Operation(
        summary = "시그널 보내기",
        description = "다른 회원에게 시그널을 보냅니다. 자기 자신에게는 보낼 수 없으며, 최근 상태에 따라 제한이 있을 수 있습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)",
        requestBody = SwaggerRequestBody(
            required = true,
            content = [Content(schema = Schema(implementation = SendSignalRequest::class))]
        )
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "시그널 전송 성공",
                content = [Content(schema = Schema(implementation = SignalResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 비즈니스 규칙 위반"),
            ApiResponse(responseCode = "404", description = "대상 회원 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun sendSignal(
        @Parameter(hidden = true) @LoginMember fromMember: Member,
        @RequestBody request: SendSignalRequest
    ): ResponseEntity<SignalResponse>

    @Operation(
        summary = "내가 받은 시그널 목록 조회",
        description = "내가 받은 시그널(신호) 목록을 페이징하여 조회합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = SignalMemberResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "인증 실패"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun getReceiveSignalForMe(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<SignalMemberResponse>>

    @Operation(
        summary = "내가 보낸 시그널 목록 조회",
        description = "내가 보낸 시그널(신호) 목록을 페이징하여 조회합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = SignalMemberResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "인증 실패"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun getSendSignalByMe(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<SignalMemberResponse>>

    @Operation(
        summary = "매칭 성공된 시그널 목록 조회",
        description = "매칭 성공된 시그널(신호) 목록을 페이징하여 조회합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공",
                content = [Content(schema = Schema(implementation = SignalMemberResponse::class))]
            ),
            ApiResponse(responseCode = "401", description = "인증 실패"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun getAcceptedSignal(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<SignalMemberResponse>>

    @Operation(
        summary = "시그널 수락",
        description = "내가 받은 시그널을 수락합니다. 이미 처리된 시그널은 수락할 수 없습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수락 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 처리된 시그널"),
            ApiResponse(responseCode = "404", description = "시그널 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun acceptSignal(
        @Parameter(hidden = true) @LoginMember me: Member,
        @PathVariable id: Long
    ): ResponseEntity<Unit>

    @Operation(
        summary = "시그널 거절",
        description = "내가 받은 시그널을 거절합니다. 이미 처리된 시그널은 거절할 수 없습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "거절 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 처리된 시그널"),
            ApiResponse(responseCode = "404", description = "시그널 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun rejectSignal(
        @Parameter(hidden = true) @LoginMember me: Member,
        @PathVariable id: Long
    ): ResponseEntity<Unit>


    @Operation(
        summary = "시그널 코드해제 조회",
        description = "코드 해제된 시그널을 조회합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "거절 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 처리된 시그널"),
            ApiResponse(responseCode = "404", description = "시그널 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun getUnlockedSignal(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ResponseEntity<Page<UnlockedMemberProfileResponse>>

    @Operation(
        summary = "시그널 숨김",
        description = "나와 관련된 시그널을 숨김 처리합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "숨김 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 처리된 시그널"),
            ApiResponse(responseCode = "404", description = "시그널 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun hideSignal(
        @Parameter(hidden = true) @LoginMember me: Member,
        @PathVariable id: Long
    ): ResponseEntity<Unit>

    @Operation(
        summary = "시그널 리스트 숨김 처리",
        description = "나와 관련된 시그널을 리스트형식으로 숨김 처리합니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "숨김 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 이미 처리된 시그널"),
            ApiResponse(responseCode = "404", description = "시그널 없음"),
            ApiResponse(responseCode = "500", description = "서버 오류")
        ]
    )
    fun hideSignals(
        @Parameter(hidden = true) @LoginMember me: Member,
        @RequestBody hideSignalRequest: HideSignalRequest
    ): ResponseEntity<Unit>
}
