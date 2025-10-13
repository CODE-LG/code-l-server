package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ReplaceImagesResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Tag(name = "프로필 심사 관리", description = "프로필 심사 거절 및 이미지 교체 API")
@RequestMapping("/v1/profile/review")
interface ProfileReviewControllerSwagger {

    @Operation(
        summary = "거절 사유 조회",
        description = """
            프로필 심사 거절 시 거절된 이미지 정보를 조회합니다.
            
            **사용 시나리오:**
            - 프로필이 REJECT 상태일 때 호출
            - 어떤 이미지가 거절되었는지 확인
            - 거절 사유를 사용자에게 전달
            
            **응답 정보:**
            - 현재 회원 상태
            - 얼굴/코드 이미지 거절 여부
            - 거절된 이미지 목록 및 사유
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "거절 정보 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ProfileRejectionInfoResponse::class),
                    examples = [ExampleObject(
                        name = "거절된 이미지가 있는 경우",
                        value = """
                        {
                          "status": "REJECT",
                          "hasFaceImageRejection": true,
                          "hasCodeImageRejection": false,
                          "rejectedFaceImages": [
                            {
                              "imageId": 123,
                              "url": "https://example.com/image1.jpg",
                              "order": 1,
                              "rejectionReason": "얼굴이 명확하게 보이지 않습니다"
                            }
                          ],
                          "rejectedCodeImages": []
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원 정보를 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    @GetMapping("/rejection-info")
    fun getRejectionInfo(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<ProfileRejectionInfoResponse>

    @Operation(
        summary = "프로필 이미지 조회",
        description = """
            프로필의 모든 이미지(얼굴, 코드)를 조회합니다.
            
            **조회 대상:**
            - 승인된 이미지
            - 거절된 이미지
            - 심사 대기 중인 이미지
            
            **응답 정보:**
            - 이미지 ID, URL, 순서
            - 승인 여부
            - 거절된 경우 거절 사유
            
            **활용:**
            - 프로필 편집 화면에서 현재 이미지 표시
            - 거절된 이미지 확인 및 교체 UI 구성
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이미지 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ProfileImagesResponse::class),
                    examples = [ExampleObject(
                        name = "혼합된 이미지 상태",
                        value = """
                        {
                          "faceImages": [
                            {
                              "imageId": 101,
                              "url": "https://example.com/face1.jpg",
                              "order": 1,
                              "isApproved": true,
                              "rejectionReason": null
                            },
                            {
                              "imageId": 102,
                              "url": "https://example.com/face2.jpg",
                              "order": 2,
                              "isApproved": false,
                              "rejectionReason": "얼굴이 명확하게 보이지 않습니다"
                            }
                          ],
                          "codeImages": [
                            {
                              "imageId": 201,
                              "url": "https://example.com/code1.jpg",
                              "order": 1,
                              "isApproved": true,
                              "rejectionReason": null
                            }
                          ]
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원 정보를 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    @GetMapping("/images")
    fun getProfileImages(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<ProfileImagesResponse>

    @Operation(
        summary = "거절된 이미지 교체",
        description = """
            거절된 이미지를 새로운 이미지로 교체합니다.
            
            **요청 제약사항:**
            - 얼굴 이미지: 정확히 2개
            - 코드 이미지: 1~3개
            - 거절된 이미지 타입만 교체 가능
            - 둘 다 거절된 경우 한 번에 모두 교체 가능
            
            **처리 과정:**
            1. 기존 거절된 이미지 삭제
            2. 새로운 이미지 업로드
            3. 프로필 상태를 PENDING으로 변경
            4. 관리자 재심사 대기
            
            **주의사항:**
            - 거절되지 않은 이미지 타입 전송 시 에러 발생
            - 이미지 개수 미충족 시 에러 발생
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "이미지 교체 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ReplaceImagesResponse::class),
                    examples = [ExampleObject(
                        name = "교체 성공",
                        value = """
                        {
                          "uploadedCount": 3,
                          "profileStatus": "PENDING",
                          "message": "이미지가 성공적으로 교체되었습니다. 관리자 승인을 기다려주세요."
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (이미지 개수 부족, 거절되지 않은 타입 전송 등)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "이미지 개수 오류",
                        value = """
                        {
                          "message": "얼굴 이미지는 정확히 2개가 필요합니다"
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "401",
                description = "인증 실패",
                content = [Content(schema = Schema(hidden = true))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "회원 정보를 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    @PutMapping("/images", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun replaceImages(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(
            description = "교체할 얼굴 이미지 (정확히 2개, 얼굴 이미지가 거절된 경우만 필수)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "faceImages", required = false) faceImages: List<MultipartFile>?,
        @Parameter(
            description = "교체할 코드 이미지 (1~3개, 코드 이미지가 거절된 경우만 필수)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "codeImages", required = false) codeImages: List<MultipartFile>?
    ): ResponseEntity<ReplaceImagesResponse>
}