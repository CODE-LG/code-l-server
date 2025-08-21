package codel.member.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.Period

data class EssentialProfileRequest(
    @field:NotBlank(message = "닉네임을 입력해주세요")
    val codeName: String,
    
    @field:NotBlank(message = "생년월일을 입력해주세요")
    @field:Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "생년월일 형식: YYYY-MM-DD")
    val birthDate: String,
    
    @field:NotBlank(message = "시/도를 선택해주세요")
    val bigCity: String,
    
    @field:NotBlank(message = "시/군/구를 선택해주세요")
    val smallCity: String,
    
    @field:NotBlank(message = "직업을 선택해주세요")
    val jobCategory: String,
    
    @field:Size(min = 1, max = 5, message = "관심사는 1-5개 사이여야 합니다")
    val interests: List<String>
) {
    fun validateSelf() {
        // 관심사 개별 검증
        interests.forEach { interest ->
            require(interest.isNotBlank() && interest.length <= 20) {
                "각 관심사는 1-20자 사이여야 합니다: '$interest'"
            }
        }
        
        // 나이 검증
        val birthDate = try {
            LocalDate.parse(this.birthDate)
        } catch (_: Exception) {
            throw IllegalArgumentException("생년월일 형식이 올바르지 않습니다")
        }
        
        val age = Period.between(birthDate, LocalDate.now()).years
        require(age in 19..99) {
            "나이는 19-99세 사이여야 합니다 (현재: ${age}세)"
        }
    }
}
