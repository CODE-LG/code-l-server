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
        val memberId = member.id ?: throw MemberException(HttpStatus.BAD_REQUEST, "id가 없는 멤버 입니다.")
        val memberEntity = findMemberEntityByMemberId(memberId)
        if (memberEntity.profileEntity == null) {
            saveProfileEntity(memberEntity, member.profile)
        }

        if (memberEntity.rejectReasonEntity == null) {
            saveRejectReasonEntity(memberEntity, member.rejectReason)
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
        memberEntity.profileEntity = savedProfileEntity
    }

    private fun saveRejectReasonEntity(
        memberEntity: MemberEntity,
        rejectReason: String?,
    ) {
        rejectReason ?: return
        val savedRejectReasonEntity = rejectReasonJpaRepository.save(RejectReasonEntity(reason = rejectReason))
        memberEntity.rejectReasonEntity = savedRejectReasonEntity
    }

    @Transactional(readOnly = true)
    fun findPendingMembers(): List<Member> {
        val memberEntities = memberJpaRepository.findByMemberStatus(MemberStatus.PENDING)

        return memberEntities.map { memberEntity -> memberEntity.toDomain() }
    }
}
