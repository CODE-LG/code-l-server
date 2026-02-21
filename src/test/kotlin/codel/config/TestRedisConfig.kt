package codel.config

import org.mockito.Mockito.mock
import org.redisson.api.RedissonClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer

@Configuration
class TestRedisConfig {

    @Bean
    @Primary
    fun redisConnectionFactory(): RedisConnectionFactory = mock(RedisConnectionFactory::class.java)

    @Bean
    @Primary
    fun redisTemplate(): RedisTemplate<String, String> = mock(RedisTemplate::class.java) as RedisTemplate<String, String>

    @Bean
    @Primary
    fun redisMessageListenerContainer(): RedisMessageListenerContainer = mock(RedisMessageListenerContainer::class.java)

    @Bean
    @Primary
    fun redissonClient(): RedissonClient = mock(RedissonClient::class.java)

    @Bean
    @Primary
    fun redisMessageRelay(): RedisMessageRelay {
        val relay = mock(RedisMessageRelay::class.java)
        return relay
    }
}
