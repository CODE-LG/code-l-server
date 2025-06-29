package codel.member.domain

import codel.member.exception.MemberException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
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
    @OneToOne(mappedBy = "member")
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
}
