package codel.config

import codel.config.argumentresolver.WebSocketMemberArgumentResolver
import codel.config.interceptor.ChatRoomSubscriptionInterceptor
import codel.config.interceptor.JwtConnectInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val chatRoomSubscriptionInterceptor: ChatRoomSubscriptionInterceptor,
    private val jwtConnectInterceptor: JwtConnectInterceptor,
    private val webSocketMemberArgumentResolver: WebSocketMemberArgumentResolver,
) : WebSocketMessageBrokerConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(webSocketMemberArgumentResolver)
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(chatRoomSubscriptionInterceptor)
        registration.interceptors(jwtConnectInterceptor)
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOrigins("*")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/sub")
        registry.setApplicationDestinationPrefixes("/pub")
    }
}
