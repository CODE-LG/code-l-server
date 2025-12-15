package codel.member.presentation.swagger

import codel.config.argumentresolver.LoginMember
import codel.member.domain.Member
import codel.member.presentation.response.ProfileImagesResponse
import codel.member.presentation.response.ProfileRejectionInfoResponse
import codel.member.presentation.response.ReplaceImagesResponse
import codel.member.presentation.response.ResubmitProfileResponse
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile

@Tag(name = "프로필 심사 관리", description = "프로필 심사 거절 및 이미지 교체 API")
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

    fun getProfileImages(
        @Parameter(hidden = true) @LoginMember member: Member
    ): ResponseEntity<ProfileImagesResponse>

    @Operation(
        summary = "거절된 이미지 교체",
        description = """
            거절된 이미지를 새로운 이미지로 교체합니다.
            
            **기존 이미지 유지 기능 추가:**
            - existingFaceImageIds, existingCodeImageIds를 통해 유지할 이미지 지정 가능
            - 지정된 이미지는 유지하고, 나머지는 새 이미지로 대체
            - 예: 얼굴 이미지 3개 중 1개만 교체하고 싶다면, 유지할 2개의 ID를 전달하고 새 이미지 1개 업로드
            
            **요청 제약사항:**
            - 얼굴 이미지: 총 2개 (유지 + 신규)
            - 코드 이미지: 총 1~3개 (유지 + 신규)
            - 거절된 이미지 타입만 교체 가능
            
            **처리 과정:**
            1. existingIds에 없는 기존 이미지 삭제
            2. 새로운 이미지 업로드
            3. 유지할 이미지 + 새 이미지로 프로필 구성
            4. 프로필 상태를 PENDING으로 변경
            5. 관리자 재심사 대기
            
            **사용 예시:**
            - 얼굴 이미지 2개 중 1개만 교체: existingFaceImageIds=[123], faceImages=1개
            - 코드 이미지 3개 중 2개 교체: existingCodeImageIds=[456], codeImages=2개
            - 전체 교체: existingIds 생략, 모든 이미지 업로드
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
                          "uploadedCount": 1,
                          "profileStatus": "PENDING",
                          "message": "얼굴 이미지 2개 (유지: 1개, 신규: 1개). 심사가 다시 진행됩니다"
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (이미지 개수 부족 등)",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "이미지 개수 오류",
                        value = """
                        {
                          "message": "얼굴 이미지는 총 2개여야 합니다. (현재: 유지 1개 + 신규 0개 = 1개)"
                        }
                        """
                    )]
                )]
            )
        ]
    )

    fun replaceImages(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(
            description = "교체할 얼굴 이미지 (얼굴 이미지가 거절된 경우, 유지할 이미지 수 + 신규 이미지 수 = 2)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "faceImages", required = false) faceImages: List<MultipartFile>?,
        @Parameter(
            description = "교체할 코드 이미지 (코드 이미지가 거절된 경우, 유지할 이미지 수 + 신규 이미지 수 = 1~3)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "codeImages", required = false) codeImages: List<MultipartFile>?,
        @Parameter(
            description = "유지할 얼굴 이미지 ID 목록 (선택사항, 콤마로 구분. 예: 1,2,3)"
        )
        @RequestParam(value = "existingFaceImageIds", required = false) existingFaceImageIds: List<Long>?,
        @Parameter(
            description = "유지할 코드 이미지 ID 목록 (선택사항, 콤마로 구분. 예: 10,11,12)"
        )
        @RequestParam(value = "existingCodeImageIds", required = false) existingCodeImageIds: List<Long>?
    ): ResponseEntity<ReplaceImagesResponse>

    @Operation(
        summary = "재심사 통합 제출 (코드/얼굴/인증 이미지)",
        description = """
            프로필 재심사를 위한 모든 이미지를 한 번에 제출합니다.

            **사용 시나리오:**
            - REJECT 상태에서 재심사 요청
            - 코드/얼굴 이미지 + 본인 인증 이미지를 한 번에 제출
            - 기존 이미지 유지 + 신규 이미지 추가 가능

            **요청 제약사항:**
            - 회원 상태: REJECT 상태여야 함
            - 얼굴 이미지: 총 2개 (유지 + 신규)
            - 코드 이미지: 총 1~3개 (유지 + 신규)
            - 인증 이미지: 필수 (1개)
            - standardImageId: 표준 인증 이미지 ID 필수

            **처리 과정:**
            1. 얼굴/코드 이미지 처리 (유지할 이미지 + 신규 이미지)
            2. 기존 인증 이미지 소프트 삭제
            3. 새 인증 이미지 업로드 및 저장
            4. 회원 상태를 HIDDEN_COMPLETED으로 변경
            5. 관리자 재심사 대기

            **기존 이미지 유지:**
            - existingFaceImageIds: 유지할 얼굴 이미지 ID 목록
            - existingCodeImageIds: 유지할 코드 이미지 ID 목록
            - 지정하지 않으면 모든 이미지 교체
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "재심사 제출 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = ResubmitProfileResponse::class),
                    examples = [ExampleObject(
                        name = "재심사 제출 완료",
                        value = """
                        {
                          "status": "HIDDEN_COMPLETED",
                          "message": "얼굴 이미지 2개 반영. 코드 이미지 2개 반영. 본인 인증 이미지 제출 완료. 심사 대기 상태로 변경되었습니다"
                        }
                        """
                    )]
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [Content(
                    mediaType = "application/json",
                    examples = [ExampleObject(
                        name = "상태 오류",
                        value = """
                        {
                          "message": "재심사 요청은 REJECT 상태에서만 가능합니다. 현재 상태: PENDING"
                        }
                        """
                    ), ExampleObject(
                        name = "이미지 개수 오류",
                        value = """
                        {
                          "message": "얼굴 이미지는 총 2개여야 합니다. (현재: 유지 1개 + 신규 0개 = 1개)"
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
                description = "회원 또는 표준 인증 이미지를 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun resubmitProfile(
        @Parameter(hidden = true) @LoginMember member: Member,
        @Parameter(
            description = "얼굴 이미지 (신규 업로드, 유지할 이미지 수 + 신규 이미지 수 = 2)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "faceImages", required = false) faceImages: List<MultipartFile>?,
        @Parameter(
            description = "코드 이미지 (신규 업로드, 유지할 이미지 수 + 신규 이미지 수 = 1~3)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "codeImages", required = false) codeImages: List<MultipartFile>?,
        @Parameter(
            description = "유지할 얼굴 이미지 ID 목록 (선택사항, 콤마로 구분)"
        )
        @RequestParam(value = "existingFaceImageIds", required = false) existingFaceImageIds: List<Long>?,
        @Parameter(
            description = "유지할 코드 이미지 ID 목록 (선택사항, 콤마로 구분)"
        )
        @RequestParam(value = "existingCodeImageIds", required = false) existingCodeImageIds: List<Long>?,
        @Parameter(
            description = "표준 인증 이미지 ID (필수)"
        )
        @RequestParam(value = "standardImageId") standardImageId: Long,
        @Parameter(
            description = "본인 인증 이미지 (필수)",
            content = [Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)]
        )
        @RequestPart(value = "verificationImage") verificationImage: MultipartFile
    ): ResponseEntity<ResubmitProfileResponse>
}