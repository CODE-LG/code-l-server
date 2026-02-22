package codel.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

/**
 * RedisMessageRelay 통합 테스트 (Testcontainers 사용)
 *
 * 검증 대상:
 * - 실제 Redis를 통한 서버 간 메시지 중계
 * - Redis Pub/Sub가 실제로 동작하는지 검증
 *
 * 실제 객체 사용:
 * - Redis: Testcontainers로 실제 Redis 컨테이너 실행
 * - RedisTemplate: 실제 Redis에 연결
 * - RedisMessageListenerContainer: 실제 채널 구독
 * - ObjectMapper: 직렬화/역직렬화
 *
 * Mock 사용:
 * - SimpMessagingTemplate: WebSocket 세션 불가 (외부 시스템 경계)
 *
 * 주의: Docker가 실행 중이어야 합니다. Docker가 없으면 @Disabled로 건너뜁니다.
 */
@Disabled("Docker가 필요한 통합 테스트 - 수동 실행 필요")
class RedisMessageRelayIntegrationTest {

    companion object {
        private lateinit var redisContainer: GenericContainer<*>
        private lateinit var connectionFactory: LettuceConnectionFactory

        @JvmStatic
        @BeforeAll
        fun setupRedis() {
            // Redis 컨테이너 시작
            redisContainer = GenericContainer(DockerImageName.parse("redis:7-alpine"))
                .withExposedPorts(6379)
            redisContainer.start()

            // Redis 연결 팩토리 생성
            val config = RedisStandaloneConfiguration(
                redisContainer.host,
                redisContainer.getMappedPort(6379)
            )
            connectionFactory = LettuceConnectionFactory(config)
            connectionFactory.afterPropertiesSet()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            connectionFactory.destroy()
            redisContainer.stop()
        }

        private fun createRedisTemplate(): RedisTemplate<String, String> {
            val template = RedisTemplate<String, String>()
            template.connectionFactory = connectionFactory
            template.keySerializer = StringRedisSerializer()
            template.valueSerializer = StringRedisSerializer()
            template.afterPropertiesSet()
            return template
        }

        private fun createListenerContainer(): RedisMessageListenerContainer {
            val container = RedisMessageListenerContainer()
            container.setConnectionFactory(connectionFactory)
            container.afterPropertiesSet()
            container.start()
            return container
        }
    }

    @Test
    fun `서버A가 publish하면 서버B의 convertAndSend가 호출된다`() {
        // given
        val objectMapper = ObjectMapper().registerKotlinModule()

        // 서버A 설정
        val messagingTemplateA = mock<SimpMessagingTemplate>()
        val redisTemplateA = createRedisTemplate()
        val listenerContainerA = createListenerContainer()
        val relayA = RedisMessageRelay(
            redisTemplate = redisTemplateA,
            messagingTemplate = messagingTemplateA,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainerA
        )

        // 서버B 설정 (다른 serverId)
        val messagingTemplateB = mock<SimpMessagingTemplate>()
        val redisTemplateB = createRedisTemplate()
        val listenerContainerB = createListenerContainer()
        val relayB = RedisMessageRelay(
            redisTemplate = redisTemplateB,
            messagingTemplate = messagingTemplateB,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainerB
        )

        // 서버B가 Redis 채널 구독
        listenerContainerB.addMessageListener(relayB, ChannelTopic("codel:websocket"))

        // 리스너 구독 완료 대기
        Thread.sleep(500)

        // when
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Hello from server A")
        relayA.publish(destination, payload)

        // then
        // 서버A의 로컬 전송 확인
        verify(messagingTemplateA).convertAndSend(destination, payload)

        // 서버B가 Redis를 통해 메시지를 받아서 전송했는지 확인 (비동기이므로 timeout 사용)
        // payload는 JSON 문자열로 전달됨
        argumentCaptor<String>().apply {
            verify(messagingTemplateB, timeout(3000)).convertAndSend(eq(destination), capture())
            assert(firstValue.contains("Hello from server A"))
        }

        // 정리
        listenerContainerA.stop()
        listenerContainerB.stop()
    }

    @Test
    fun `서버A가 publish해도 자기 자신은 다시 처리하지 않는다`() {
        // given
        val objectMapper = ObjectMapper().registerKotlinModule()
        val messagingTemplate = mock<SimpMessagingTemplate>()
        val redisTemplate = createRedisTemplate()
        val listenerContainer = createListenerContainer()

        val relay = RedisMessageRelay(
            redisTemplate = redisTemplate,
            messagingTemplate = messagingTemplate,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainer
        )

        // 자기 자신의 메시지도 구독 (실제로는 같은 serverId 메시지는 무시됨)
        listenerContainer.addMessageListener(relay, ChannelTopic("codel:websocket"))

        // 리스너 구독 완료 대기
        Thread.sleep(500)

        // when
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Hello")
        relay.publish(destination, payload)

        // 비동기 메시지 처리 대기
        Thread.sleep(1000)

        // then
        // publish 내부에서 1번만 호출되고, Redis를 통해 다시 받은 메시지는 무시되어야 함
        verify(messagingTemplate, times(1)).convertAndSend(destination, payload)

        // 정리
        listenerContainer.stop()
    }

    @Test
    fun `다중 서버 환경에서 하나의 메시지가 모든 서버에 전달된다`() {
        // given
        val objectMapper = ObjectMapper().registerKotlinModule()

        // 서버A
        val messagingTemplateA = mock<SimpMessagingTemplate>()
        val redisTemplateA = createRedisTemplate()
        val listenerContainerA = createListenerContainer()
        val relayA = RedisMessageRelay(
            redisTemplate = redisTemplateA,
            messagingTemplate = messagingTemplateA,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainerA
        )

        // 서버B
        val messagingTemplateB = mock<SimpMessagingTemplate>()
        val redisTemplateB = createRedisTemplate()
        val listenerContainerB = createListenerContainer()
        val relayB = RedisMessageRelay(
            redisTemplate = redisTemplateB,
            messagingTemplate = messagingTemplateB,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainerB
        )
        listenerContainerB.addMessageListener(relayB, ChannelTopic("codel:websocket"))

        // 서버C
        val messagingTemplateC = mock<SimpMessagingTemplate>()
        val redisTemplateC = createRedisTemplate()
        val listenerContainerC = createListenerContainer()
        val relayC = RedisMessageRelay(
            redisTemplate = redisTemplateC,
            messagingTemplate = messagingTemplateC,
            objectMapper = objectMapper,
            messageListenerContainer = listenerContainerC
        )
        listenerContainerC.addMessageListener(relayC, ChannelTopic("codel:websocket"))

        // 리스너 구독 완료 대기
        Thread.sleep(500)

        // when
        val destination = "/sub/v1/chatroom/1"
        val payload = mapOf("message" to "Broadcast to all servers")
        relayA.publish(destination, payload)

        // then
        // 서버A는 로컬 전송
        verify(messagingTemplateA).convertAndSend(destination, payload)

        // 서버B와 C는 Redis를 통해 전송받음 (payload는 JSON 문자열)
        argumentCaptor<String>().apply {
            verify(messagingTemplateB, timeout(3000)).convertAndSend(eq(destination), capture())
            assert(firstValue.contains("Broadcast to all servers"))
        }
        argumentCaptor<String>().apply {
            verify(messagingTemplateC, timeout(3000)).convertAndSend(eq(destination), capture())
            assert(firstValue.contains("Broadcast to all servers"))
        }

        // 정리
        listenerContainerA.stop()
        listenerContainerB.stop()
        listenerContainerC.stop()
    }
}