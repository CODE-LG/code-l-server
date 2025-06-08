package codel.config.argumentresolver

import codel.auth.exception.AuthException
import codel.member.business.MemberService
import codel.member.domain.Member
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver
import org.springframework.messaging.simp.SimpMessageHeaderAccessor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.stereotype.Component

@Component
class WebSocketMemberArgumentResolver(
    private val memberService: MemberService,
) : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(LoginMember::class.java) &&
            parameter.parameterType == Member::class.java

    override fun resolveArgument(
        parameter: MethodParameter,
        message: Message<*>,
    ): Any? {
        val headerAccessor = MessageHeaderAccessor.getAccessor(message, SimpMessageHeaderAccessor::class.java)
        val sessionAttributes = headerAccessor?.sessionAttributes
        val memberId =
            sessionAttributes?.get("memberId") as? String
                ?: throw AuthException(HttpStatus.UNAUTHORIZED, "memberId가 요청에 없습니다.")

        return memberService.findMember(memberId.toLong())
    }
}
