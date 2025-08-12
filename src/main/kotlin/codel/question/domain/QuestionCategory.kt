package codel.question.domain

enum class QuestionCategory(
    val displayName: String,
    val description: String
) {
    VALUES("가치관", "인생 가치관·성향"),
    FAVORITE("취향", "취향·관심사·콘텐츠"),
    CURRENT_ME("요즘 나", "최근 상태·몰입한 것"),
    DATE("데이트", "사람 대할 때 나의 방식"),
    MEMORY("추억", "감동·전환점·경험 공유"),
    WANT_TALK("이런 대화 해보고 싶어", "나누고 싶은 진짜 이야기"),
    BALANCE_ONE("하나만", "가벼운 밸런스 게임");

    companion object {
        fun fromString(category: String?): QuestionCategory? {
            return values().find { it.name.equals(category, ignoreCase = true) }
        }
    }
}