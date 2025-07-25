package codel.block.presentation.swagger

import codel.block.presentation.request.BlockMemberRequest
import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Block", description = "차단 관련 API")
interface BlockControllerSwagger {

    @Operation(
        summary = "회원 차단",
        description = "특정 회원을 차단합니다. 이미 차단했거나 자기 자신을 차단할 수는 없습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "차단 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신 차단, 중복 차단 등)"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun blockMember(
        @Parameter(hidden = true) @LoginMember blocker: Member,
        @RequestBody blockMemberRequest: BlockMemberRequest
    ): ResponseEntity<Unit>

    @Operation(
        summary = "회원 차단 해제",
        description = "특정 회원을 차단 해제합니다. 이미 차단했거나 자기 자신을 차단할 수는 없습니다. (※ Authorization 헤더에 JWT를 포함시켜야 합니다.)"
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "차단 해제 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (자기 자신 차단, 중복 차단 등)"),
            ApiResponse(responseCode = "500", description = "서버 내부 오류"),
        ],
    )
    fun unBlockMember(
        @Parameter(hidden = true) @LoginMember blocker: Member,
        @PathVariable memberId : Long,
    ): ResponseEntity<Unit>
}