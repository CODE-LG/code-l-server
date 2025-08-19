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

    @Operation(
        summary = "코드해제 요청 승인",
        description = """
            대기 중인 코드해제 요청을 승인합니다.
            
            **기능 설명:**
            - 상대방의 코드해제 요청을 승인하여 서로의 프로필을 공개
            - 승인 시 채팅방 상태가 UNLOCKED로 변경됨
            - 채팅방에 승인 완료 시스템 메시지가 전송됨
            
            **제약 조건:**
            - PENDING 상태인 요청만 승인 가능
            - 본인의 요청은 승인할 수 없음
            - 해당 채팅방의 멤버만 승인 가능
            
            **처리 결과:**
            1. 코드해제 요청 상태가 APPROVED로 변경
            2. 채팅방 isUnlocked = true, status = UNLOCKED로 변경
            3. "코드해제가 승인되었습니다" 시스템 메시지 전송
            4. 양쪽 사용자에게 실시간 알림 전송
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", 
                description = "코드해제 요청 승인 성공"
            ),
            ApiResponse(
                responseCode = "400", 
                description = """
                요청이 잘못되었습니다. 가능한 오류:
                - 이미 처리된 요청 (PENDING 상태가 아님)
                - 본인의 요청을 승인하려고 시도
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
                description = "코드해제 요청을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "500", 
                description = "서버 내부 오류"
            ),
        ],
    )
    fun approveUnlock(
        @Parameter(hidden = true) @LoginMember processor: Member,
        @Parameter(
            description = "승인할 코드해제 요청 ID", 
            required = true, 
            example = "456"
        )
        @PathVariable requestId: Long
    ): ResponseEntity<UnlockRequestResponse>

    @Operation(
        summary = "코드해제 요청 거절",
        description = """
            대기 중인 코드해제 요청을 거절합니다.
            
            **기능 설명:**
            - 상대방의 코드해제 요청을 거절
            - 채팅방 상태는 LOCKED 상태 유지
            - 채팅방에 거절 완료 시스템 메시지가 전송됨
            
            **제약 조건:**
            - PENDING 상태인 요청만 거절 가능
            - 본인의 요청은 거절할 수 없음
            - 해당 채팅방의 멤버만 거절 가능
            
            **처리 결과:**
            1. 코드해제 요청 상태가 REJECTED로 변경
            2. 채팅방 상태는 변경되지 않음 (여전히 LOCKED)
            3. "코드해제 요청이 거절되었습니다" 시스템 메시지 전송
            4. 양쪽 사용자에게 실시간 알림 전송
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200", 
                description = "코드해제 요청 거절 성공"
            ),
            ApiResponse(
                responseCode = "400", 
                description = """
                요청이 잘못되었습니다. 가능한 오류:
                - 이미 처리된 요청 (PENDING 상태가 아님)
                - 본인의 요청을 거절하려고 시도
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
                description = "코드해제 요청을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "500", 
                description = "서버 내부 오류"
            ),
        ],
    )
    fun rejectUnlock(
        @Parameter(hidden = true) @LoginMember processor: Member,
        @Parameter(
            description = "거절할 코드해제 요청 ID", 
            required = true, 
            example = "456"
        )
        @PathVariable requestId: Long
    ): ResponseEntity<UnlockRequestResponse>
}
