package codel.config.filter

import codel.config.Loggable
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(2)
class MemberLoggingFilter :
    OncePerRequestFilter(),
    Loggable {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val memberId = request.getAttribute("memberId")?.toString()
        if (memberId != null) {
            MDC.put("memberId", memberId)
        }
        try {
            log.info { "HTTP ${request.method} ${request.requestURI} memberId=$memberId" }
            filterChain.doFilter(request, response)
        } finally {
            MDC.clear()
        }
    }
}
