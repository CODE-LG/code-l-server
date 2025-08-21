package codel.member.presentation.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PersonalityProfileRequest(
    val hairLength: String?,
    val bodyType: String?,
    
    @field:Min(value = 120, message = "키는 120cm 이상이어야 합니다")
    @field:Max(value = 220, message = "키는 220cm 이하여야 합니다")
    val height: Int?,
    
    @field:Size(max = 5, message = "스타일은 최대 5개까지 선택 가능합니다")
    val styles: List<String>,
    
    @field:Pattern(regexp = "[A-Z]{4}", message = "MBTI는 4자리 대문자 영문이어야 합니다")
    val mbti: String?,
    
    val drinkingStyle: String?,
    val smokingStyle: String?,
    
    @field:Size(min = 1, max = 5, message = "성격은 1-5개 사이여야 합니다")
    val personalities: List<String>,
    
    val questionId: Long?,
    
    val answer: String?
) {
    fun validateSelf() {
        // 성격 개별 검증
        personalities.forEach { personality ->
            require(personality.isNotBlank() && personality.length <= 15) {
                "각 성격은 1-15자 사이여야 합니다: '$personality'"
            }
        }
        
        // 질문/답변 쌍 검증
        val hasQuestionId = questionId != null
        val hasAnswer = !answer.isNullOrBlank()
        require(hasQuestionId == hasAnswer) {
            "대표 질문과 답변은 함께 입력해야 합니다"
        }
    }
}
