package codel.member.domain

enum class MemberStatus {
    SIGNUP,                    // 회원가입 (기존)
    PHONE_VERIFIED,           // 1단계: 전화번호 인증 완료 (신규)
    ESSENTIAL_COMPLETED,      // 2단계: 기본 프로필 완료 (기존 CODE_SURVEY 대체)
    PERSONALITY_COMPLETED,    // 3단계: 성격/취향 프로필 완료 (신규)
    HIDDEN_COMPLETED,         // 4단계: 히든 프로필 완료, 인증 이미지 제출 대기 (기존 CODE_PROFILE_IMAGE 대체)
    PENDING,                  // 관리자 심사 중 (기존)
    REJECT,                   // 심사 거절 (기존)
    DONE,                     // 최종 승인 완료 (기존)
    WITHDRAWN,                // 회원탈퇴 (신규)
    ADMIN,
}
