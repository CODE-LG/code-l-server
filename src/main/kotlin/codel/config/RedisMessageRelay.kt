package codel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.annotation.PostConstruct
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class RedisMessageRelay(
    private val redisTemplate: RedisTemplate<String, String>,
    private val messagingTemplate: SimpMessagingTemplate,
    private val objectMapper: ObjectMapper,
    private val messageListenerContainer: RedisMessageListenerContainer
) : MessageListener, Loggable {

    private val serverId = System.getenv("HOSTNAME") ?: "server-${System.currentTimeMillis()}"

    private val processedMessages: Cache<String, Boolean> = Caffeine.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .maximumSize(10_000)
        .build()

    @PostConstruct
    fun initialize() {
        val topic = ChannelTopic("codel:websocket")
        messageListenerContainer.addMessageListener(this, topic)
        log.info { "Redis WebSocket relay initialized — serverId: $serverId" }
    }

    fun publish(destination: String, payload: Any) {
        messagingTemplate.convertAndSend(destination, payload)

        try {
            val message = DistributedMessage(
                id = "$serverId-${System.nanoTime()}",
                serverId = serverId,
                destination = destination,
                payload = objectMapper.writeValueAsString(payload)
            )
            redisTemplate.convertAndSend("codel:websocket", objectMapper.writeValueAsString(message))
        } catch (e: Exception) {
            log.error { "Redis broadcast 실패 — 로컬 전송은 완료됨: ${e.message}" }
        }
    }

    override fun onMessage(message: Message, pattern: ByteArray?) {
        try {
            val distributed = objectMapper.readValue(String(message.body), DistributedMessage::class.java)

            if (distributed.serverId == serverId) return

            if (processedMessages.getIfPresent(distributed.id) != null) return
            processedMessages.put(distributed.id, true)

            messagingTemplate.convertAndSend(distributed.destination, distributed.payload)
        } catch (e: Exception) {
            log.error { "Redis 메시지 처리 실패: ${e.message}" }
        }
    }

    data class DistributedMessage(
        val id: String,
        val serverId: String,
        val destination: String,
        val payload: String
    )
}
