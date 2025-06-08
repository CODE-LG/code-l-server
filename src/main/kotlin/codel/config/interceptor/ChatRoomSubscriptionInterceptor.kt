package codel.config.interceptor

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

        if (StompCommand.SUBSCRIBE == accessor.command) {
            val sessionAttributes = accessor.sessionAttributes
            val memberId = sessionAttributes?.get("memberId")?.toString()

            val originalDestination = accessor.destination
            if (originalDestination == "/sub/v1/chatrooms/user" && memberId != null) {
                val newDestination = "$originalDestination/$memberId"
                accessor.destination = newDestination
            }
        }

        return message
    }
}
