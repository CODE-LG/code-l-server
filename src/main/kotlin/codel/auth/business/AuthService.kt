package codel.auth.business

import codel.auth.TokenProvider
import codel.member.domain.Member
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val tokenProvider: TokenProvider,
) {
    fun provideToken(member: Member): String = tokenProvider.provide(member)
}
