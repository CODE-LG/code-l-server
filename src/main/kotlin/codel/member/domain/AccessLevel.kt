package codel.member.domain

enum class AccessLevel(val description: String) {
    SELF("본인 프로필"),
    CODE_EXCHANGED("코드 교환 완료"),
    PUBLIC("일반 공개 프로필");

    fun canViewHidden(): Boolean {
        return this in listOf(SELF, CODE_EXCHANGED)
    }
    
    fun canEdit(): Boolean {
        return this == SELF
    }
}
