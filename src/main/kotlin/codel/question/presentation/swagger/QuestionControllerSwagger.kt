package codel.question.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.question.presentation.response.QuestionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Question", description = "질문 관련 API")
interface QuestionControllerSwagger {

    @Operation(
        summary = "활성화된 질문 목록 조회",
        description = """
            현재 활성화된 모든 질문을 카테고리별로 조회합니다.
            
            **카테고리 설명:**
            - VALUES: 가치관 관련 질문 (인생 가치관·성향)
            - FAVORITE: 취향 관련 질문 (취향·관심사·콘텐츠)
            - CURRENT_ME: 현재 상태 관련 질문 (최근 상태·몰입한 것)
            - DATE: 데이트/관계 관련 질문 (사람 대할 때 나의 방식)
            - MEMORY: 추억/경험 관련 질문 (감동·전환점·경험 공유)
            - WANT_TALK: 대화 주제 관련 질문 (나누고 싶은 진짜 이야기)
            - BALANCE_ONE: 밸런스 게임 관련 질문 (가벼운 밸런스 게임)
            
            ※ Authorization 헤더에 JWT 토큰을 포함해야 합니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "질문 목록 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    array = ArraySchema(schema = Schema(implementation = QuestionResponse::class))
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증되지 않은 사용자 - JWT 토큰이 없거나 유효하지 않음",
                content = [Content()]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [Content()]
            )
        ]
    )
    fun findActiveQuestion(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<List<QuestionResponse>>
}
