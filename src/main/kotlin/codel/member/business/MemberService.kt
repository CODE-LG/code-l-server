package codel.member.business

import codel.member.domain.CodeImage
import codel.member.domain.DailySeedProvider
import codel.member.domain.FaceImage
import codel.member.domain.ImageUploader
import codel.member.domain.Member
import codel.member.domain.MemberRepository
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.domain.Profile
import codel.member.infrastructure.MemberJpaRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Transactional
@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val imageUploader: ImageUploader,
    private val memberJpaRepository: MemberJpaRepository,
) {
    fun loginMember(member: Member): Member {
        val loginMember = memberRepository.loginMember(member)

        return loginMember
    }

    // TODO: 설문에서 저장 받을 때만 고려함, 프로필 수정 고려 필요
    fun saveProfile(
        member: Member,
        profile: Profile,
    ) {
        val saveProfile = memberRepository.saveProfile(profile)
        memberRepository.updateMemberProfile(member, saveProfile)
    }

    @Transactional(readOnly = true)
    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member = memberRepository.findMember(oauthType, oauthId)

    @Transactional(readOnly = true)
    fun findMember(memberId: Long): Member = memberRepository.findMember(memberId)

    fun saveCodeImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val codeImage = uploadCodeImage(files)
        val serializeCodeImages = codeImage.serializeAttribute()
        val profile = member.profile
        if (profile != null) {
            memberRepository.updateMemberCodeImage(profile, serializeCodeImages)
        }
    }

    private fun uploadCodeImage(files: List<MultipartFile>): CodeImage = CodeImage(files.map { file -> imageUploader.uploadFile(file) })

    fun saveFaceImage(
        member: Member,
        files: List<MultipartFile>,
    ) {
        val faceImage = uploadFaceImage(files)
        val serializeCodeImages = faceImage.serializeAttribute()
        val profile = member.profile
        if (profile != null) {
            memberRepository.updateMemberFaceImage(profile, serializeCodeImages)
        }
    }

    private fun uploadFaceImage(files: List<MultipartFile>): FaceImage = FaceImage(files.map { file -> imageUploader.uploadFile(file) })

    fun saveFcmToken(
        member: Member,
        fcmToken: String,
    ) {
        memberRepository.updateMemberFcmToken(member, fcmToken)
    }

    @Transactional(readOnly = true)
    fun findPendingMembers(): List<Member> = memberRepository.findPendingMembers()

    fun approveMember(memberId: Long): Member {
        val member = memberRepository.findMember(memberId)

        member.memberStatus = MemberStatus.DONE
        return memberRepository.updateMember(member)
    }

    fun rejectMember(
        memberId: Long,
        reason: String,
    ): Member {
        val member = memberRepository.findMember(memberId)

        memberRepository.saveRejectReason(member, reason)
        member.memberStatus = MemberStatus.REJECT

        return memberRepository.updateMember(member)
    }

    @Transactional(readOnly = true)
    fun findRejectReason(member: Member): String = memberRepository.findRejectReason(member)

    @Transactional(readOnly = true)
    fun findMemberProfile(member: Member): Member {
        val memberId = member.getIdOrThrow()
        return memberRepository.findMember(memberId)
    }

    @Transactional(readOnly = true)
    fun recommendMembers(member: Member): List<Member> {
        val excludeId = member.getIdOrThrow()
        val seed = DailySeedProvider.generateDailySeedForMember(excludeId)
        return memberJpaRepository.findRandomMembers(excludeId, 5, seed)
    }

    @Transactional(readOnly = true)
    fun getRandomMembers(
        page: Int,
        size: Int,
    ): Page<Member> {
        val seed = DailySeedProvider.generateRandomSeed()
        val offset = page * size
        val members = memberJpaRepository.findMembersWithSeed(seed, size, offset)
        val total = memberJpaRepository.count()
        val pageable = PageRequest.of(page, size)

        return PageImpl(members, pageable, total)
    }
}
