package codel.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

@Configuration
@EnableAsync
class AsyncConfig {
    
    /**
     * 알림 전송용 비동기 Executor
     * 
     * - corePoolSize: 기본 스레드 수 (10개)
     * - maxPoolSize: 최대 스레드 수 (50개)
     * - queueCapacity: 대기 큐 크기 (100개)
     * - threadNamePrefix: 스레드 이름 접두사 (로그 추적용)
     */
    @Bean(name = ["notificationExecutor"])
    fun notificationExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 10
        executor.maxPoolSize = 50
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("notification-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(60)
        executor.initialize()
        return executor
    }
    
    /**
     * 일반적인 비동기 작업용 Executor
     */
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 5
        executor.maxPoolSize = 20
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("async-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()
        return executor
    }
}
