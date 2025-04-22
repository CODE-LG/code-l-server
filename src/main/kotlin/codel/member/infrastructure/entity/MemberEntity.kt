package codel.member.infrastructure.entity

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["oauthType", "oauthId"]),
    ],
)
class MemberEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private var id: Long? = null,
    @OneToOne
    var profileEntity: ProfileEntity? = null,
    var fcmToken: String? = null,
    var oauthType: OauthType,
    var oauthId: String,
    var memberStatus: MemberStatus,
) {
    companion object {
        fun toEntity(member: Member): MemberEntity =
            MemberEntity(
                oauthType = member.oauthType,
                oauthId = member.oauthId,
                memberStatus = member.memberStatus,
            )
    }

    fun toDomain(): Member =
        Member(
            id = this.id,
            profile = this.profileEntity?.toDomain(),
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            memberStatus = this.memberStatus,
            codeImage = profileEntity?.getCodeImage()?.let { CodeImage(it) },
            faceImage = profileEntity?.getFaceImage()?.let { FaceImage(it) },
        )

    fun updateEntity(
        member: Member,
        profileEntity: ProfileEntity?,
    ) {
        profileEntity?.let {
            this.profileEntity = profileEntity
        }
        member.codeImage?.let {
            this.profileEntity?.updateCodeImage(it)
        }
        member.faceImage?.let {
            this.profileEntity?.updateFaceImage(it)
        }
        member.fcmToken?.let {
            this.fcmToken = it
        }
        this.memberStatus = member.memberStatus
    }
}
