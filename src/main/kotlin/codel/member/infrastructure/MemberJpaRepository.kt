package codel.member.infrastructure

import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import codel.member.infrastructure.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun existsByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): Boolean

    fun findByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): MemberEntity

    fun findByMemberStatus(memberStatus: MemberStatus): List<MemberEntity>
}
