package codel.question.domain

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "질문 그룹", enumAsRef = true)
enum class QuestionGroup(
    @Schema(description = "그룹 표시명")
    val displayName: String
) {
    @Schema(description = "A그룹 - 가벼운/진입용 질문")
    A("A그룹"),

    @Schema(description = "B그룹 - 깊이 있는/무게감 있는 질문")
    B("B그룹"),

    @Schema(description = "그룹 구분 없음")
    RANDOM("랜덤");

    companion object {
        fun fromString(group: String?): QuestionGroup? {
            return entries.find { it.name.equals(group, ignoreCase = true) }
        }
    }
}
