package codel.member.presentation.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class PhoneVerificationRequest(
    @field:NotBlank(message = "전화번호를 입력해주세요")
    @field:Pattern(regexp = "^01[0-9]-?[0-9]{4}-?[0-9]{4}$", message = "올바른 전화번호 형식이 아닙니다")
    val phoneNumber: String,
    
    @field:NotBlank(message = "인증번호를 입력해주세요")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "인증번호는 6자리 숫자여야 합니다")
    val verificationCode: String
)
