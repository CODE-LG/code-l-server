package codel.config

import codel.config.argumentresolver.WebSocketMemberArgumentResolver
import codel.config.interceptor.ChatRoomSubscriptionInterceptor
import codel.config.interceptor.JwtConnectInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
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
        registration.taskExecutor(wsInboundExecutor())
    }

    @Bean
    fun wsInboundExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 8
            maxPoolSize = 16
            queueCapacity = 5000
            setThreadNamePrefix("ws-in-")
            initialize()
        }

    @Bean
    fun wsOutboundExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = 8
            maxPoolSize = 16
            queueCapacity = 5000
            setThreadNamePrefix("ws-out-")
            initialize()
        }
    
    override fun configureClientOutboundChannel(registration: ChannelRegistration) {
        registration.taskExecutor(wsOutboundExecutor())
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry
            .addEndpoint("/ws")
            .setAllowedOrigins("*")
    }


    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.enableSimpleBroker("/sub")
            .setHeartbeatValue(longArrayOf(10000, 10000))  // 10초마다 heartbeat
            .setTaskScheduler(heartBeatScheduler())  // 오타 수정: hearBeat → heartBeat
        registry.setApplicationDestinationPrefixes("/pub")
    }

    @Bean
    fun heartBeatScheduler(): ThreadPoolTaskScheduler {
        val scheduler = ThreadPoolTaskScheduler()
        scheduler.poolSize = 1
        scheduler.setThreadNamePrefix("ws-heartbeat-")
        scheduler.initialize()
        return scheduler
    }
}
