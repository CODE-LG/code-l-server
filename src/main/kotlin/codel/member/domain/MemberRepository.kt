package codel.member.domain

import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.infrastructure.RejectReasonJpaRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component

@Component
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository,
    private val rejectReasonJpaRepository: RejectReasonJpaRepository,
) {
    fun loginMember(member: Member): Member {
        if (memberJpaRepository.existsByOauthTypeAndOauthId(member.oauthType, member.oauthId)) {
            return findMember(member.oauthType, member.oauthId)
        }
        return try {
            memberJpaRepository.save(member)
        } catch (e: DataIntegrityViolationException) {
            throw MemberException(HttpStatus.BAD_REQUEST, "이미 회원이 존재합니다.")
        }
    }

    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member = memberJpaRepository.findByOauthTypeAndOauthId(oauthType, oauthId)

    fun findMember(memberId: Long): Member = findMemberById(memberId)

    private fun findMemberById(memberId: Long) =
        memberJpaRepository.findByIdOrNull(memberId) ?: throw MemberException(
            HttpStatus.BAD_REQUEST,
            "해당 id에 일치하는 멤버가 없습니다.",
        )

    fun findDoneMember(memberId: Long): Member {
        val member = findMemberById(memberId)
        if (member.memberStatus != MemberStatus.DONE) {
            throw MemberException(HttpStatus.BAD_REQUEST, "해당 멤버는 회원가입을 완료하지 않았습니다.")
        }
        return member
    }

    fun updateMemberProfile(
        member: Member,
        profile: Profile,
    ) {
        member.profile = profile
        memberJpaRepository.save(member)
    }

    fun saveRejectReason(
        member: Member,
        rejectReason: String,
    ) {
        val member = findMemberById(member.getIdOrThrow())
        val memberRejectReason =
            RejectReason(
                member = member,
                reason = rejectReason,
            )
        rejectReasonJpaRepository.save(memberRejectReason)
    }

    fun findPendingMembers(): List<Member> = memberJpaRepository.findByMemberStatus(MemberStatus.PENDING)

    fun findRejectReason(member: Member): String {
        member.validateRejectedOrThrow()
        val findMember = findMemberById(member.getIdOrThrow())
        val rejectReasonEntity = findRejectReasonByMemberOrThrow(findMember)

        return rejectReasonEntity.reason
    }

    private fun findRejectReasonByMemberOrThrow(member: Member): RejectReason =
        rejectReasonJpaRepository.findByMember(member)
            ?: throw MemberException(
                HttpStatus.BAD_REQUEST,
                "거절 사유가 존재하지 않는 멤버입니다.",
            )

    fun updateMember(member: Member): Member = memberJpaRepository.save(member)

    fun saveProfile(profile: Profile): Profile = profileJpaRepository.save(profile)

    fun updateMemberCodeImage(
        profile: Profile,
        serializeCodeImages: String,
    ) {
        profile.codeImage = serializeCodeImages
        profileJpaRepository.save(profile)
    }

    fun updateMemberFaceImage(
        profile: Profile,
        serializeFaceImage: String,
    ) {
        profile.faceImage = serializeFaceImage
        profileJpaRepository.save(profile)
    }

    fun updateMemberFcmToken(
        member: Member,
        fcmToken: String,
    ) {
        member.fcmToken = fcmToken
        memberJpaRepository.save(member)
    }
}
