package codel.question.domain

/**
 * 채팅방 질문 추천 시 그룹 정책
 */
enum class GroupPolicy {
    /**
     * 그룹 정책 없음 (회원가입 전용 카테고리)
     */
    NONE,

    /**
     * A그룹 우선 → B그룹 순서로 추천
     */
    A_THEN_B,

    /**
     * 그룹 구분 없이 랜덤 추천
     */
    RANDOM
}
