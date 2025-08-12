package codel.question.domain

enum class QuestionCategory(
    val displayName: String,
    val description: String,
    val priority: Int = 0
) {
    // 일반 대화용
    DAILY_LIFE("일상", "일상생활과 취미에 관한 질문", 1),
    PERSONALITY("성격", "성격과 가치관에 관한 질문", 2),
    RELATIONSHIP("관계", "인간관계와 연애에 관한 질문", 3),
    
    // 깊은 대화용
    FUTURE_DREAMS("미래와 꿈", "미래 계획과 꿈에 관한 질문", 4),
    VALUES("가치관", "인생 가치관과 철학에 관한 질문", 5),
    EXPERIENCES("경험", "특별한 경험과 추억에 관한 질문", 6),
    
    // 특별 상황용
    ICE_BREAKER("아이스브레이커", "처음 만났을 때 어색함을 깨는 질문", 10),
    DEEP_TALK("진지한 대화", "깊이 있는 대화를 위한 질문", 8),
    FUN("재미있는", "재미있고 유쾌한 질문", 7),
    
    // 시스템용
    ONBOARDING("온보딩", "회원가입 시 사용되는 질문", 0),
    EVENT("이벤트", "특별 이벤트용 질문", 9);
    
    companion object {
        fun fromString(category: String?): QuestionCategory? {
            return values().find { it.name.equals(category, ignoreCase = true) }
        }
        
        fun getActiveCategories(): List<QuestionCategory> {
            return values().filter { it != ONBOARDING }.sortedBy { it.priority }
        }
        
        fun getChatCategories(): List<QuestionCategory> {
            return listOf(DAILY_LIFE, PERSONALITY, RELATIONSHIP, FUTURE_DREAMS, VALUES, EXPERIENCES, ICE_BREAKER, DEEP_TALK, FUN)
        }
    }
}
