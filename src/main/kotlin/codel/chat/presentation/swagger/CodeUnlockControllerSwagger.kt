package codel.chat.presentation.swagger

import codel.chat.presentation.response.UnlockRequestResponse
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Code Unlock", description = "코드해제 관련 API")
interface CodeUnlockControllerSwagger {

    @Operation(
        summary = "코드해제 요청",
        description = """
            채팅방에서 상대방에게 코드해제를 요청합니다.
            
            **기능 설명:**
            - 상대방의 숨겨진 프로필 정보를 보기 위해 코드해제를 요청
            - 요청 시 채팅방에 시스템 메시지가 전송됨
            - 실시간으로 상대방에게 알림이 전송됨
            
            **제약 조건:**
            - 이미 코드가 해제된 채팅방에서는 요청 불가
            - 동일한 사용자가 중복 요청 불가 (대기 중인 요청이 있을 때)
            - 비활성화된 채팅방에서는 요청 불가
            
            **사용 시나리오:**
            1. 사용자가 채팅방에서 코드해제 버튼 클릭
            2. 이 API 호출로 요청 생성
            3. 채팅방에 "코드해제 요청이 왔습니다" 시스템 메시지 표시
            4. 상대방에게 실시간 알림 전송
            5. 상대방이 승인/거절 선택 (2단계에서 구현 예정)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", 
                description = "코드해제 요청 성공"
            ),
            ApiResponse(
                responseCode = "400", 
                description = """
                요청이 잘못되었습니다. 가능한 오류:
                - 이미 코드가 해제된 채팅방
                - 이미 대기 중인 요청이 존재
                - 비활성화된 채팅방
                """
            ),
            ApiResponse(
                responseCode = "401", 
                description = "인증되지 않은 사용자"
            ),
            ApiResponse(
                responseCode = "403", 
                description = "해당 채팅방에 접근할 권한이 없음"
            ),
            ApiResponse(
                responseCode = "404", 
                description = "채팅방을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "500", 
                description = "서버 내부 오류"
            ),
        ],
    )
    fun requestUnlock(
        @Parameter(hidden = true) @LoginMember requester: Member,
        @Parameter(
            description = "코드해제를 요청할 채팅방 ID", 
            required = true, 
            example = "123"
        )
        @PathVariable chatRoomId: Long
    ): ResponseEntity<UnlockRequestResponse>
}
