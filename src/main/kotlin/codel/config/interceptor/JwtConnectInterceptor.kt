package codel.config.interceptor

import codel.auth.TokenProvider
import codel.auth.exception.AuthException
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Component
class JwtConnectInterceptor(
    private val tokenProvider: TokenProvider,
) : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*>? {
        val accessor = StompHeaderAccessor.wrap(message)

        if (accessor.command == StompCommand.CONNECT) {
            val token = resolveToken(accessor)
            val memberId = tokenProvider.extractMemberId(token)
            accessor.sessionAttributes?.put("memberId", memberId)
        }

        return message
    }

    private fun resolveToken(accessor: StompHeaderAccessor) =
        accessor
            .getFirstNativeHeader("Authorization")
            ?.removePrefix("Bearer ")
            ?: throw AuthException(HttpStatus.BAD_REQUEST, "토큰이 존재하지 않습니다.")
}
