package codel.member.infrastructure

import codel.member.domain.Member
import codel.member.domain.Profile
import org.springframework.data.jpa.repository.JpaRepository

interface ProfileJpaRepository : JpaRepository<Profile, Long> {
    fun findByMemberId(member : Member) : Profile?
}

