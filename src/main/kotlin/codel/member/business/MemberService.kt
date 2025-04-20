package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.member.presentation.request.MemberLoginRequest
import codel.member.presentation.request.ProfileSavedRequest
import codel.member.presentation.response.MemberLoginResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val imageUploader: ImageUploader,
) {
    fun loginMember(request: MemberLoginRequest): MemberLoginResponse {
        val member =
            Member(
                oauthType = request.oauthType,
                oauthId = request.oauthId,
            )
        val loginMember = memberRepository.loginMember(member)

        return MemberLoginResponse(loginMember.memberStatus)
    }

    fun saveProfile(
        member: Member,
        request: ProfileSavedRequest,
    ) {
        val profile =
            Profile(
                codeName = request.codeName,
                age = request.age,
                job = request.job,
                alcohol = request.alcohol,
                smoke = request.smoke,
                hobby = request.hobby,
                style = request.style,
                bigCity = request.bigCity,
                smallCity = request.smallCity,
                mbti = request.mbti,
                introduce = request.introduce,
            )
        val updateMember = member.updateProfile(profile)
        memberRepository.saveMember(updateMember)
    }

    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member = memberRepository.findMember(oauthType, oauthId)

    @Transactional
    fun saveCodeImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val codeImage = uploadCodeImage(files)
        val updateMember = member.updateCodeImage(codeImage)
        memberRepository.saveMember(updateMember)
    }

    private fun uploadCodeImage(files: List<MultipartFile>): CodeImage = CodeImage(files.map { file -> imageUploader.uploadFile(file) })

    @Transactional
    fun saveFaceImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val faceImage = uploadFaceImage(files)
        val updateMember = member.updateFaceImage(faceImage)
        memberRepository.saveMember(updateMember)
    }

    private fun uploadFaceImage(files: List<MultipartFile>): FaceImage = FaceImage(files.map { file -> imageUploader.uploadFile(file) })

    fun saveFcmToken(
        member: Member,
        fcmToken: String,
    ) {
        val updateMember = member.updateFcmToken(fcmToken)
        memberRepository.saveMember(updateMember)
    }
}
