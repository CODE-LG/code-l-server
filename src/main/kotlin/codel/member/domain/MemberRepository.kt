package codel.member.domain

import codel.member.infrastructure.MemberJpaRepository
import codel.member.infrastructure.ProfileJpaRepository
import codel.member.infrastructure.entity.MemberEntity
import codel.member.infrastructure.entity.ProfileEntity
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Transactional
@Component
class MemberRepository(
    private val memberJpaRepository: MemberJpaRepository,
    private val profileJpaRepository: ProfileJpaRepository,
) {
    fun loginMember(member: Member): Member {
        if (memberJpaRepository.existsByOauthTypeAndOauthId(member.oauthType, member.oauthId)) {
            return findMember(member.oauthType, member.oauthId)
        }
        return try {
            val memberEntity = memberJpaRepository.save(MemberEntity.toEntity(member))
            memberEntity.toDomain()
        } catch (e: DataIntegrityViolationException) {
            throw IllegalArgumentException("이미 회원이 존재합니다.")
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

    fun saveMember(member: Member) {
        val memberId = member.id ?: throw IllegalArgumentException("id가 없는 멤버 입니다.")
        val memberEntity = memberJpaRepository.findByIdOrNull(memberId) ?: throw IllegalArgumentException("해당 id 멤버가 존재하지 않습니다.")
        val profileEntity = member.profile?.let { profileJpaRepository.save(ProfileEntity.toEntity(member.profile)) }

        memberEntity.updateEntity(member, profileEntity)
    }
}
