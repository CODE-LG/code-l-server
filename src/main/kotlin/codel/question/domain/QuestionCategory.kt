package codel.question.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 카테고리", enumAsRef = true)
enum class QuestionCategory(
    @Schema(description = "카테고리 표시명")
    val displayName: String,
    @Schema(description = "카테고리 상세 설명")
    val description: String,
    @Schema(description = "회원가입에서 사용 여부")
    val usedInSignup: Boolean,
    @Schema(description = "채팅방에서 사용 여부")
    val usedInChat: Boolean,
    @Schema(description = "채팅방 그룹 정책")
    val chatGroupPolicy: GroupPolicy
) {
    // 회원가입 전용
    @Schema(description = "가치관 관련 질문 (회원가입 전용)")
    VALUES(
        displayName = "가치관",
        description = "인생 가치관·성향",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    // 회원가입 전용
    @Schema(description = "취향 관련 질문")
    FAVORITE(
        displayName = "favorite",
        description = "취향·관심사·콘텐츠",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    @Schema(description = "현재 상태 관련 질문")
    CURRENT_ME(
        displayName = "요즘 나",
        description = "최근 상태·몰입한 것",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    @Schema(description = "데이트/관계 관련 질문")
    DATE(
        displayName = "데이트",
        description = "사람 대할 때 나의 방식",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    @Schema(description = "추억/경험 관련 질문")
    MEMORY(
        displayName = "추억",
        description = "감동·전환점·경험 공유",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    @Schema(description = "대화 주제 관련 질문")
    WANT_TALK(
        displayName = "이런대화해보고싶어",
        description = "나누고 싶은 진짜 이야기",
        usedInSignup = true,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    @Schema(description = "밸런스 게임 관련 질문 (레거시)")
    BALANCE_ONE(
        displayName = "하나만",
        description = "가벼운 밸런스 게임",
        usedInSignup = false,
        usedInChat = false,
        chatGroupPolicy = GroupPolicy.NONE
    ),

    // 채팅방 전용
    @Schema(description = "가치관 코드 - 가치관으로 서로의 성격 코드 알아가기")
    VALUES_CODE(
        displayName = "가치관 코드",
        description = "가치관으로 서로의 성격 코드 알아가기",
        usedInSignup = false,
        usedInChat = true,
        chatGroupPolicy = GroupPolicy.A_THEN_B
    ),

    @Schema(description = "텐션업 코드 - 가벼운 선택 질문")
    TENSION_UP(
        displayName = "텐션업 코드",
        description = "가벼운 선택 질문으로 텐션은 올리고 부담은 줄이기",
        usedInSignup = false,
        usedInChat = true,
        chatGroupPolicy = GroupPolicy.RANDOM
    ),

    @Schema(description = "만약에 코드 - 가정 상황 질문")
    IF(
        displayName = "만약에 코드",
        description = "상황을 가정하며 자연스럽게 서로의 성격 코드 알아가기",
        usedInSignup = false,
        usedInChat = true,
        chatGroupPolicy = GroupPolicy.A_THEN_B
    ),

    @Schema(description = "비밀 코드(19+) - 민감한 주제 질문")
    SECRET(
        displayName = "비밀 코드(19+)",
        description = "먼저 묻기 민망한 취향과 텐션을 조심스럽고 솔직하게",
        usedInSignup = false,
        usedInChat = true,
        chatGroupPolicy = GroupPolicy.A_THEN_B
    );

    fun isChatCategory(): Boolean = usedInChat
    fun isSignupCategory(): Boolean = usedInSignup

    companion object {
        fun fromString(category: String?): QuestionCategory? {
            return entries.find { it.name.equals(category, ignoreCase = true) }
        }

        fun getSignupCategories(): List<QuestionCategory> =
            entries.filter { it.usedInSignup }

        fun getChatCategories(): List<QuestionCategory> =
            entries.filter { it.usedInChat }
    }
}
