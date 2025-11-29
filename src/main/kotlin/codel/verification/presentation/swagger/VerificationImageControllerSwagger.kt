package codel.verification.presentation.swagger

import codel.verification.presentation.response.StandardVerificationImageResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity

@Tag(name = "Verification Image", description = "인증 이미지 관련 API")
interface VerificationImageControllerSwagger {

    @Operation(
        summary = "표준 인증 이미지 랜덤 조회",
        description = """
            회원 인증을 위한 표준 포즈 이미지를 랜덤으로 하나 조회합니다.

            사용자는 표준 이미지를 보고 동일한 자세로 본인 사진을 촬영하여 제출해야 합니다.

            **사용 시점:**
            - 회원가입 플로우에서 VERIFICATION_IMAGE 상태일 때
            - 심사 거절 후 재제출 시

            **반환 데이터:**
            - 활성화된(isActive=true) 표준 이미지 중 랜덤으로 하나 반환

            **응답 예시:**
            ```json
            {
              "id": 1,
              "imageUrl": "https://s3.../standard_verification_images/pose1.jpg",
              "description": "정면을 보고 양손을 귀 옆에 올려주세요"
            }
            ```
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "표준 인증 이미지 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = StandardVerificationImageResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류 - 활성화된 표준 이미지가 없거나 조회 실패",
                content = [Content()]
            )
        ]
    )
    fun getRandomStandardImage(): ResponseEntity<StandardVerificationImageResponse>
}