package codel.member.domain

import codel.member.exception.MemberException
import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.infrastructure.RejectReasonJpaRepository
import codel.member.infrastructure.entity.MemberEntity
import codel.member.infrastructure.entity.ProfileEntity
import codel.member.infrastructure.entity.RejectReasonEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
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
            val memberEntity = memberJpaRepository.save(MemberEntity.toEntity(member))
            memberEntity.toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw MemberException(HttpStatus.BAD_REQUEST, "이미 회원이 존재합니다.")
        }
    }

    @Transactional(readOnly = true)
    fun findMember(
        oauthType: OauthType,
        oauthId: String,
    ): Member {
        val memberEntity = memberJpaRepository.findByOauthTypeAndOauthId(oauthType, oauthId)
        return memberEntity.toDomain()
    }

    @Transactional(readOnly = true)
    fun findMember(memberId: Long): Member {
        val memberEntity = findMemberEntityByMemberId(memberId)
        return memberEntity.toDomain()
    }

    fun updateMember(member: Member) {
        val memberEntity = findMemberEntityByMemberId(member.getIdOrThrow())
        if (memberEntity.profileEntity == null) {
            saveProfileEntity(memberEntity, member.profile)
        }
        memberEntity.updateEntity(member)
    }

    private fun findMemberEntityByMemberId(memberId: Long) =
        memberJpaRepository.findByIdOrNull(memberId) ?: throw MemberException(
            HttpStatus.BAD_REQUEST,
            "해당 id에 일치하는 멤버가 없습니다.",
        )

    private fun saveProfileEntity(
        memberEntity: MemberEntity,
        profile: Profile?,
    ) {
        profile ?: return
        val savedProfileEntity = profileJpaRepository.save(ProfileEntity.toEntity(profile))
        memberEntity.saveProfileEntity(savedProfileEntity)
    }

    fun saveRejectReason(
        member: Member,
        rejectReason: String,
    ) {
        val memberEntity = findMemberEntityByMemberId(member.getIdOrThrow())
        val rejectReasonEntity = RejectReasonEntity.toEntity(memberEntity, rejectReason)
        rejectReasonJpaRepository.save(rejectReasonEntity)
    }

    @Transactional(readOnly = true)
    fun findPendingMembers(): List<Member> {
        val memberEntities = memberJpaRepository.findByMemberStatus(MemberStatus.PENDING)

        return memberEntities.map { memberEntity -> memberEntity.toDomain() }
    }

    @Transactional(readOnly = true)
    fun findRejectReason(member: Member): String {
        member.validateRejectedOrThrow()
        val memberEntity = findMemberEntityByMemberId(member.getIdOrThrow())
        val rejectReasonEntity = findByMemberEntityOrThrow(memberEntity)

        return rejectReasonEntity.reason
    }

    private fun findByMemberEntityOrThrow(memberEntity: MemberEntity): RejectReasonEntity =
        rejectReasonJpaRepository.findByMemberEntity(memberEntity)
            ?: throw MemberException(
                HttpStatus.BAD_REQUEST,
                "거절 사유가 존재하지 않는 멤버입니다.",
            )
}
