package codel.notification.business

import codel.config.Loggable
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * 비동기 알림 전송 서비스
 * 
 * - 대량 알림을 병렬로 빠르게 처리
 * - CompletableFuture를 사용하여 결과 추적 가능
 * - 배치 처리 지원
 */
@Service
class AsyncNotificationService(
    private val notificationService: NotificationService
) : IAsyncNotificationService, Loggable {
    
    /**
     * 단일 알림을 비동기로 전송
     * 
     * @return CompletableFuture<NotificationResult>
     */
    @Async("notificationExecutor")
    override fun sendAsync(notification: Notification): CompletableFuture<NotificationResult> {
        return try {
            notificationService.send(notification)
            CompletableFuture.completedFuture(
                NotificationResult.success(
                    targetId = notification.targetId ?: "unknown",
                    type = notification.type
                )
            )
        } catch (e: Exception) {
            log.warn(e) { "❌ 비동기 알림 전송 실패 - targetId: ${notification.targetId}" }
            CompletableFuture.completedFuture(
                NotificationResult.failure(
                    targetId = notification.targetId ?: "unknown",
                    type = notification.type,
                    error = e.message ?: "Unknown error"
                )
            )
        }
    }
    
    /**
     * 여러 알림을 비동기 배치로 전송
     * 
     * @param notifications 전송할 알림 리스트
     * @return CompletableFuture<BatchNotificationResult>
     */
    override fun sendBatchAsync(notifications: List<Notification>): CompletableFuture<BatchNotificationResult> {
        val startTime = System.currentTimeMillis()
        
        // 모든 알림을 비동기로 전송
        val futures = notifications.map { notification ->
            sendAsync(notification)
        }
        
        // 모든 작업이 완료될 때까지 대기
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply {
                val results = futures.map { it.join() }
                val duration = System.currentTimeMillis() - startTime
                
                BatchNotificationResult(
                    total = notifications.size,
                    success = results.count { it.success },
                    failure = results.count { !it.success },
                    results = results,
                    durationMs = duration
                )
            }
    }
    
    /**
     * FCM 전용: 배치 처리
     * FCM은 최대 500개씩 배치로 전송 가능
     */
    @Async("notificationExecutor")
    override fun sendFcmBatchAsync(
        tokens: List<String>,
        title: String,
        body: String
    ): CompletableFuture<BatchNotificationResult> {
        val startTime = System.currentTimeMillis()
        
        // 500개씩 청크로 나누기
        val chunks = tokens.chunked(500)
        
        log.info { "📦 FCM 배치 전송 시작 - 총 ${tokens.size}개를 ${chunks.size}개 배치로 분할" }
        
        val futures = chunks.map { chunk ->
            sendFcmChunkAsync(chunk, title, body)
        }
        
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply {
                val results = futures.flatMap { it.join().results }
                val duration = System.currentTimeMillis() - startTime
                
                val successCount = results.count { it.success }
                val failureCount = results.count { !it.success }
                
                log.info { 
                    "✅ FCM 배치 전송 완료 - " +
                    "성공: $successCount, 실패: $failureCount, " +
                    "소요시간: ${duration}ms"
                }
                
                BatchNotificationResult(
                    total = tokens.size,
                    success = successCount,
                    failure = failureCount,
                    results = results,
                    durationMs = duration
                )
            }
    }
    
    /**
     * FCM 청크 단위 전송 (최대 500개)
     */
    private fun sendFcmChunkAsync(
        tokens: List<String>,
        title: String,
        body: String
    ): CompletableFuture<BatchNotificationResult> {
        val notifications = tokens.map { token ->
            Notification(
                type = NotificationType.MOBILE,
                targetId = token,
                title = title,
                body = body
            )
        }
        
        return sendBatchAsync(notifications)
    }
}

/**
 * 단일 알림 전송 결과
 */
data class NotificationResult(
    val targetId: String,
    val type: NotificationType,
    val success: Boolean,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun success(targetId: String, type: NotificationType) = NotificationResult(
            targetId = targetId,
            type = type,
            success = true
        )
        
        fun failure(targetId: String, type: NotificationType, error: String) = NotificationResult(
            targetId = targetId,
            type = type,
            success = false,
            error = error
        )
    }
}

/**
 * 배치 알림 전송 결과
 */
data class BatchNotificationResult(
    val total: Int,
    val success: Int,
    val failure: Int,
    val results: List<NotificationResult>,
    val durationMs: Long
) {
    val successRate: Double
        get() = if (total > 0) (success.toDouble() / total) * 100 else 0.0
    
    fun getFailedTargets(): List<String> {
        return results.filter { !it.success }.map { it.targetId }
    }
    
    fun getErrorSummary(): Map<String, Int> {
        return results
            .filter { !it.success }
            .groupBy { it.error ?: "Unknown error" }
            .mapValues { it.value.size }
    }
}
