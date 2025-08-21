package codel.member.domain

import codel.member.exception.MemberException
import jakarta.persistence.*
import org.springframework.http.HttpStatus

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["oauthType", "oauthId"]),
    ],
)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    var email: String,
    @OneToOne(mappedBy = "member", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var profile: Profile? = null,
    var fcmToken: String? = null,
    @Enumerated(EnumType.STRING)
    var oauthType: OauthType,
    var oauthId: String,
    @Enumerated(EnumType.STRING)
    var memberStatus: MemberStatus,
) {
    fun getIdOrThrow(): Long = id ?: throw MemberException(HttpStatus.BAD_REQUEST, "id가 없는 멤버 입니다.")

    fun getProfileOrThrow(): Profile = profile ?: throw MemberException(HttpStatus.BAD_REQUEST, "프로필이 없는 멤버입니다.")

    fun validateRejectedOrThrow() {
        takeIf { memberStatus == MemberStatus.REJECT }
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "심사 거절된 멤버가 아닙니다.")
    }

    fun isNotDone(): Boolean = memberStatus != MemberStatus.DONE

    fun updateProfile(profile: Profile) {
        this.profile = profile
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Member) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    // ===== 회원가입 단계 검증 및 상태 전환 메서드들 =====
    
    /**
     * 전화번호 인증을 완료하고 상태를 PHONE_VERIFIED로 변경
     */
    fun completePhoneVerification() {
        require(memberStatus == MemberStatus.SIGNUP) {
            "전화번호 인증은 회원가입 직후에만 가능합니다. 현재 상태: $memberStatus"
        }
        memberStatus = MemberStatus.PHONE_VERIFIED
    }

    /**
     * Essential Profile 등록이 가능한지 확인
     */
    fun canProceedToEssential(): Boolean {
        return memberStatus == MemberStatus.PHONE_VERIFIED
    }

    /**
     * Essential Profile 등록 가능 여부 검증
     */
    fun validateCanProceedToEssential() {
        require(memberStatus == MemberStatus.PHONE_VERIFIED) {
            "Essential Profile 등록은 전화번호 인증 완료 후에만 가능합니다. 현재 상태: $memberStatus"
        }
    }

    /**
     * Essential Profile 완료 상태로 변경
     */
    fun completeEssentialProfile() {
        validateCanProceedToEssential()
        memberStatus = MemberStatus.ESSENTIAL_COMPLETED
    }

    /**
     * Personality Profile 등록이 가능한지 확인
     */
    fun canProceedToPersonality(): Boolean {
        return memberStatus == MemberStatus.ESSENTIAL_COMPLETED
    }

    /**
     * Personality Profile 등록 가능 여부 검증
     */
    fun validateCanProceedToPersonality() {
        require(memberStatus == MemberStatus.ESSENTIAL_COMPLETED) {
            "Personality Profile 등록은 Essential Profile 완료 후에만 가능합니다. 현재 상태: $memberStatus"
        }
    }

    /**
     * Personality Profile 완료 상태로 변경
     */
    fun completePersonalityProfile() {
        validateCanProceedToPersonality()
        memberStatus = MemberStatus.PERSONALITY_COMPLETED
    }

    /**
     * Hidden Profile 등록이 가능한지 확인
     */
    fun canProceedToHidden(): Boolean {
        return memberStatus == MemberStatus.PERSONALITY_COMPLETED
    }

    /**
     * Hidden Profile 등록 가능 여부 검증
     */
    fun validateCanProceedToHidden() {
        require(memberStatus == MemberStatus.PERSONALITY_COMPLETED) {
            "Hidden Profile 등록은 Personality Profile 완료 후에만 가능합니다. 현재 상태: $memberStatus"
        }
    }

    /**
     * Hidden Profile 완료 상태로 변경 (최종 단계 - PENDING 상태로)
     */
    fun completeHiddenProfile() {
        validateCanProceedToHidden()
        memberStatus = MemberStatus.PENDING
    }

    /**
     * 현재 상태에서 다음 진행 가능한 단계 반환
     */
    fun getNextAvailableStep(): MemberStatus? {
        return when (memberStatus) {
            MemberStatus.SIGNUP -> MemberStatus.PHONE_VERIFIED
            MemberStatus.PHONE_VERIFIED -> MemberStatus.ESSENTIAL_COMPLETED
            MemberStatus.ESSENTIAL_COMPLETED -> MemberStatus.PERSONALITY_COMPLETED
            MemberStatus.PERSONALITY_COMPLETED -> MemberStatus.HIDDEN_COMPLETED
            MemberStatus.HIDDEN_COMPLETED -> MemberStatus.PENDING
            else -> null
        }
    }


}
