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

    fun registerProfile(profile: Profile) {
        this.profile = profile
        profile.member = this
        this.memberStatus = MemberStatus.CODE_SURVEY
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


}
