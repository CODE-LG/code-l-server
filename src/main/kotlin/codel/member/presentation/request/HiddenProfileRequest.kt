package codel.member.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class HiddenProfileRequest(
    @field:NotBlank(message = "사랑의 언어를 선택해주세요")
    val loveLanguage: String,
    
    @field:NotBlank(message = "애정 표현 스타일을 선택해주세요")
    val affectionStyle: String,
    
    @field:NotBlank(message = "연락 스타일을 선택해주세요")
    val contactStyle: String,
    
    @field:NotBlank(message = "데이트 스타일을 선택해주세요")
    val dateStyle: String,
    
    @field:NotBlank(message = "갈등 해결 스타일을 선택해주세요")
    val conflictResolutionStyle: String,
    
    @field:NotBlank(message = "연애 가치관을 선택해주세요")
    val relationshipValues: String,
    
    @field:Size(min = 2, max = 3, message = "얼굴 이미지는 2-3장 사이여야 합니다")
    val faceImages: List<String>
)
