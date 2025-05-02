package codel.member.domain

import codel.member.exception.MemberException
import org.springframework.http.HttpStatus

class Member(
    val id: Long? = null,
    val profile: Profile? = null,
    val email: String,
    val oauthType: OauthType,
    val oauthId: String,
    val codeImage: CodeImage? = null,
    val faceImage: FaceImage? = null,
    val memberStatus: MemberStatus = MemberStatus.SIGNUP,
    val fcmToken: String? = null,
) {
    fun updateProfile(profile: Profile): Member =
        Member(
            id = this.id,
            profile = profile,
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            codeImage = this.codeImage,
            faceImage = this.faceImage,
            memberStatus = MemberStatus.CODE_SURVEY,
            fcmToken = this.fcmToken,
            email = this.email,
        )

    fun updateCodeImage(codeImage: CodeImage): Member =
        Member(
            id = this.id,
            profile = this.profile,
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            codeImage = codeImage,
            faceImage = this.faceImage,
            memberStatus = MemberStatus.CODE_PROFILE_IMAGE,
            fcmToken = this.fcmToken,
            email = this.email,
        )

    fun updateFaceImage(faceImage: FaceImage): Member =
        Member(
            id = this.id,
            profile = this.profile,
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            codeImage = this.codeImage,
            faceImage = faceImage,
            memberStatus = MemberStatus.PENDING,
            fcmToken = this.fcmToken,
            email = this.email,
        )

    fun updateFcmToken(fcmToken: String): Member =
        Member(
            id = this.id,
            profile = this.profile,
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            codeImage = this.codeImage,
            faceImage = this.faceImage,
            memberStatus = this.memberStatus,
            fcmToken = fcmToken,
            email = this.email,
        )

    fun updateMemberStatus(memberStatus: MemberStatus): Member =
        Member(
            id = this.id,
            profile = this.profile,
            oauthType = this.oauthType,
            oauthId = this.oauthId,
            codeImage = this.codeImage,
            faceImage = this.faceImage,
            memberStatus = memberStatus,
            fcmToken = this.fcmToken,
            email = this.email,
        )

    fun getIdOrThrow(): Long = id ?: throw MemberException(HttpStatus.BAD_REQUEST, "id가 없는 멤버 입니다.")

    fun validateRejectedOrThrow() {
        takeIf { memberStatus == MemberStatus.REJECT }
            ?: throw MemberException(HttpStatus.BAD_REQUEST, "심사 거절된 멤버가 아닙니다.")
    }
}
