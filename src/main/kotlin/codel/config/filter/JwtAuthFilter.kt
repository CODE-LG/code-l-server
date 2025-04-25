package codel.config.filter

import codel.auth.TokenProvider
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(1)
class JwtAuthFilter(
    private val tokenProvider: TokenProvider,
) : OncePerRequestFilter() {
    companion object {
        private val EXCLUDE_URIS =
            listOf(
                "/v1/member/login",
                "/v1/admin/login",
                "/v1/health",
                "/swagger-ui/",
                "/v3/api-docs",
                "/actuator/",
                "/image/",
                "/favicon.ico",
            )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        if (EXCLUDE_URIS.any { request.requestURI.startsWith(it) }) {
            filterChain.doFilter(request, response)
            return
        }

        val token = resolveToken(request)

        if (token == null || !tokenProvider.validateToken(token)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.characterEncoding = "UTF-8"
            response.writer.write("""{"message": "인증되지 않은 사용자입니다."}""")
            return
        }
        val memberId = tokenProvider.extractMemberId(token)
        request.setAttribute("memberId", memberId)

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val bearer = request.getHeader("Authorization")
        return if (bearer != null && bearer.startsWith("Bearer ")) {
            bearer.substring(7)
        } else {
            findTokenInCookie(request)
        }
    }

    private fun findTokenInCookie(request: HttpServletRequest): String? {
        val cookies = request.cookies
        val accessToken = cookies?.firstOrNull { it.name == "access_token" }
        return accessToken?.value
    }
}
