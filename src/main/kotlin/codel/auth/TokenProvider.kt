package codel.auth

import codel.auth.exception.AuthException
import codel.member.domain.Member
import codel.member.domain.OauthType
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import java.security.Key
import java.util.Date

@Component
class TokenProvider(
    @Value("\${security.jwt.token.secret-key}")
    private val secretKey: String,
    @Value("\${security.jwt.token.expire-length}")
    private val validityInMilliseconds: Long,
) {
    companion object {
        private const val MEMBER_ID_CLAIM_KEY = "id"
        private const val SOCIAL_LOGIN_ID_CLAIM_KEY = "oauthId"
        private const val OAUTH_TYPE = "oauthType"
    }

    private val key: Key
        get() = Keys.hmacShaKeyFor(secretKey.toByteArray())

    fun provide(member: Member): String {
        val now = Date()
        val validity = Date(now.time + validityInMilliseconds)

        return Jwts
            .builder()
            .claim(MEMBER_ID_CLAIM_KEY, member.id)
            .claim(SOCIAL_LOGIN_ID_CLAIM_KEY, member.oauthId)
            .claim(OAUTH_TYPE, member.oauthType)
            .setIssuedAt(now)
            .setExpiration(validity)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun validateToken(token: String): Boolean =
        runCatching {
            val claims = getPayload(token)
            validateExpireTime(claims)
        }.isSuccess

    private fun getPayload(token: String): Claims =
        try {
            Jwts
                .parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .body
        } catch (e: Exception) {
            throw AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.")
        }

    private fun validateExpireTime(claims: Claims) {
        if (claims.expiration.before(Date())) {
            throw AuthException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.")
        }
    }

    fun extractOauthId(token: String): String = getPayload(token)[SOCIAL_LOGIN_ID_CLAIM_KEY].toString()

    fun extractOauthType(token: String): OauthType = OauthType.valueOf(getPayload(token)[OAUTH_TYPE].toString())

    fun extractMemberId(token: String): String = getPayload(token)[MEMBER_ID_CLAIM_KEY].toString()
}
