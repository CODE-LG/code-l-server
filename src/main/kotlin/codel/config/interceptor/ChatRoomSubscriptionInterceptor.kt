package codel.config.interceptor

import codel.auth.exception.AuthException
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.stereotype.Component

@Component
class ChatRoomSubscriptionInterceptor : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)

        if (StompCommand.SUBSCRIBE == accessor.command || StompCommand.UNSUBSCRIBE == accessor.command) {
            val memberId = findMemberIdFromSession(accessor)

            val originalDestination = accessor.destination
            if (originalDestination == "/sub/v1/chatrooms/member") {
                val newDestination = "$originalDestination/$memberId"
                accessor.destination = newDestination
            }
        }

        return message
    }

    private fun findMemberIdFromSession(accessor: StompHeaderAccessor) =
        accessor.sessionAttributes?.get("memberId")
            ?: throw AuthException(HttpStatus.BAD_REQUEST, "사용자 아이디가 세션에 존재하지 않습니다.")
}
