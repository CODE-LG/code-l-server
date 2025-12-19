package codel.config.argumentresolver

import codel.auth.exception.AuthException
import codel.member.business.MemberService
import codel.member.domain.Member
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

@Component
class MemberArgumentResolver(
    private val memberService: MemberService,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginMember::class.java) &&
                parameter.parameterType == Member::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val httpServletRequest =
            webRequest.getNativeRequest(HttpServletRequest::class.java)
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "HttpServletRequest를 가져올 수 없습니다.")

        val memberId =
            httpServletRequest.getAttribute("memberId") as? String
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "memberId가 요청이 없습니다.")

        val member = memberService.findMember(memberId.toLong())
        
        // 탈퇴한 회원 차단
        if (member.isWithdrawn()) {
            throw AuthException(HttpStatus.UNAUTHORIZED, "탈퇴한 회원입니다.")
        }
        
        return member
    }
}
