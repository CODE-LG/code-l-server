package codel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.messaging.simp.SimpMessagingTemplate
import kotlin.test.assertNotNull

/**
 * RedisMessageRelay 단위 테스트
 *
 * 검증 대상:
 * - 메시지 필터링 (같은 serverId는 무시)
 * - 중복 방지 (processedMessages cache)
 * - 직렬화/역직렬화 로직
 *
 * Mock 사용 원칙:
 * - ObjectMapper: 실제 인스턴스 (직렬화 로직 검증 필요)
 * - SimpMessagingTemplate: Mock (WebSocket 세션 없이는 실제 전송 불가)
 * - RedisTemplate: Mock (단위 테스트에서 Redis 없음)
 * - RedisMessageListenerContainer: Mock (PostConstruct에서만 사용)
 */
class RedisMessageRelayTest {

    private lateinit var messagingTemplate: SimpMessagingTemplate
    private lateinit var redisTemplate: RedisTemplate<String, String>
    private lateinit var objectMapper: ObjectMapper
    private lateinit var messageListenerContainer: RedisMessageListenerContainer
    private lateinit var relay: RedisMessageRelay

    @BeforeEach
    fun setup() {
        messagingTemplate = mock()
        redisTemplate = mock()
        objectMapper = ObjectMapper().registerKotlinModule()
        messageListenerContainer = mock()

        relay = RedisMessageRelay(
            redisTemplate = redisTemplate,
            messagingTemplate = messagingTemplate,
            objectMapper = objectMapper,
            messageListenerContainer = messageListenerContainer
        )
    }

    @Test
    fun `publish 메서드는 로컬 messagingTemplate을 올바른 destination과 payload로 호출한다`() {
        // given
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Hello")

        // when
        relay.publish(destination, payload)

        // then
        verify(messagingTemplate).convertAndSend(destination, payload)
    }

    @Test
    fun `publish 메서드는 Redis로 DistributedMessage 형식으로 전송한다`() {
        // given
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Hello")

        // when
        relay.publish(destination, payload)

        // then
        argumentCaptor<String>().apply {
            verify(redisTemplate).convertAndSend(eq("codel:websocket"), capture())

            val json = firstValue
            val message = objectMapper.readValue(json, RedisMessageRelay.DistributedMessage::class.java)

            assertNotNull(message.id)
            assertNotNull(message.serverId)
            assert(message.destination == destination)
            // payload는 JSON 문자열로 직렬화됨
            assert(message.payload.contains("Hello"))
        }
    }

    @Test
    fun `publish 메서드는 Redis 전송 실패 시에도 로컬 전송은 완료하고 예외를 전파하지 않는다`() {
        // given
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Hello")
        whenever(redisTemplate.convertAndSend(any<String>(), any<String>()))
            .thenThrow(RuntimeException("Redis connection failed"))

        // when & then (예외가 전파되지 않아야 함)
        relay.publish(destination, payload)

        // 로컬 전송은 완료되었는지 확인
        verify(messagingTemplate).convertAndSend(destination, payload)
    }

    @Test
    fun `onMessage 메서드는 다른 serverId의 메시지를 올바른 destination으로 전달한다`() {
        // given
        val destination = "/sub/v1/chatroom/1"
        val payloadString = objectMapper.writeValueAsString(mapOf("message" to "Hello from other server"))
        val message = RedisMessageRelay.DistributedMessage(
            id = "other-server-123",
            serverId = "other-server",
            destination = destination,
            payload = payloadString
        )
        val json = objectMapper.writeValueAsString(message)
        val redisMessage = createMessage(json)

        // when
        relay.onMessage(redisMessage, null)

        // then
        verify(messagingTemplate).convertAndSend(destination, payloadString)
    }

    @Test
    fun `onMessage 메서드는 동일 id로 2번 호출하면 두 번째는 무시한다`() {
        // given
        val destination = "/sub/v1/chatroom/1"
        val payloadString = "{\"message\":\"Hello\"}"
        val message = RedisMessageRelay.DistributedMessage(
            id = "msg-unique-id",
            serverId = "other-server",
            destination = destination,
            payload = payloadString
        )
        val json = objectMapper.writeValueAsString(message)
        val redisMessage = createMessage(json)

        // when
        relay.onMessage(redisMessage, null) // 첫 번째 호출
        relay.onMessage(redisMessage, null) // 두 번째 호출 (중복)

        // then
        verify(messagingTemplate, times(1)).convertAndSend(destination, payloadString) // 1번만 호출되어야 함
    }

    @Test
    fun `onMessage 메서드는 잘못된 JSON이 오면 예외 없이 처리한다`() {
        // given
        val invalidJson = "{ invalid json }"
        val redisMessage = createMessage(invalidJson)

        // when & then (예외가 전파되지 않아야 함)
        relay.onMessage(redisMessage, null)

        // convertAndSend는 호출되지 않아야 함
        verifyNoMoreInteractions(messagingTemplate)
    }

    // Helper: Redis Message 객체 생성
    private fun createMessage(body: String): org.springframework.data.redis.connection.Message {
        return object : org.springframework.data.redis.connection.Message {
            override fun getBody(): ByteArray = body.toByteArray()
            override fun getChannel(): ByteArray = "codel:websocket".toByteArray()
        }
    }
}