package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val imageUploader: ImageUploader,
) {
    fun loginMember(member: Member): MemberStatus {
        val loginMember = memberRepository.loginMember(member)

        return loginMember.memberStatus
    }

    fun saveProfile(
        member: Member,
        profile: Profile,
    ) {
        val updateMember = member.updateProfile(profile)
        memberRepository.updateMember(updateMember)
    }

    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member = memberRepository.findMember(oauthType, oauthId)

    fun findMember(memberId: Long): Member = memberRepository.findMember(memberId)

    @Transactional
    fun saveCodeImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val codeImage = uploadCodeImage(files)
        val updateMember = member.updateCodeImage(codeImage)
        memberRepository.updateMember(updateMember)
    }

    private fun uploadCodeImage(files: List<MultipartFile>): CodeImage = CodeImage(files.map { file -> imageUploader.uploadFile(file) })

    @Transactional
    fun saveFaceImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val faceImage = uploadFaceImage(files)
        val updateMember = member.updateFaceImage(faceImage)
        memberRepository.updateMember(updateMember)
    }

    private fun uploadFaceImage(files: List<MultipartFile>): FaceImage = FaceImage(files.map { file -> imageUploader.uploadFile(file) })

    fun saveFcmToken(
        member: Member,
        fcmToken: String,
    ) {
        val updateMember = member.updateFcmToken(fcmToken)
        memberRepository.updateMember(updateMember)
    }
}
