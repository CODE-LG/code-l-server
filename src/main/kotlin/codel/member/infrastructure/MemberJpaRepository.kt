package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MemberJpaRepository : JpaRepository<Member, Long> {
    fun existsByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): Boolean

    fun findByOauthTypeAndOauthId(
        oauthType: OauthType,
        oauthId: String,
    ): Member

    fun findByMemberStatus(memberStatus: MemberStatus): List<Member>
}
