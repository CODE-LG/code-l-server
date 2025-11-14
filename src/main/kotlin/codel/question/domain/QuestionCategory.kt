package codel.question.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 카테고리", enumAsRef = true)
enum class QuestionCategory(
    @Schema(description = "카테고리 표시명")
    val displayName: String,
    @Schema(description = "카테고리 상세 설명")
    val description: String
) {
    @Schema(description = "가치관 관련 질문")
    VALUES("가치관", "인생 가치관·성향"),
    
    @Schema(description = "취향 관련 질문")
    FAVORITE("취향", "취향·관심사·콘텐츠"),
    
    @Schema(description = "현재 상태 관련 질문")
    CURRENT_ME("요즘 나", "최근 상태·몰입한 것"),
    
    @Schema(description = "데이트/관계 관련 질문")
    DATE("데이트", "사람 대할 때 나의 방식"),
    
    @Schema(description = "추억/경험 관련 질문")
    MEMORY("추억", "감동·전환점·경험 공유"),
    
    @Schema(description = "대화 주제 관련 질문")
    WANT_TALK("이런 대화 해보고 싶어", "나누고 싶은 진짜 이야기"),
    
    @Schema(description = "밸런스 게임 관련 질문")
    BALANCE_ONE("하나만", "가벼운 밸런스 게임"),

    @Schema(description = "가정 상황 관련 질문")
    IF("만약에", "가상의 상황·선택 질문");

    companion object {
        fun fromString(category: String?): QuestionCategory? {
            return values().find { it.name.equals(category, ignoreCase = true) }
        }
    }
}