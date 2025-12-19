package codel.notification.business

import codel.notification.domain.Notification
import java.util.concurrent.CompletableFuture

/**
 * 비동기 알림 전송 서비스 인터페이스
 */
interface IAsyncNotificationService {
    
    /**
     * 단일 알림을 비동기로 전송
     */
    fun sendAsync(notification: Notification): CompletableFuture<NotificationResult>
    
    /**
     * 여러 알림을 비동기 배치로 전송
     */
    fun sendBatchAsync(notifications: List<Notification>): CompletableFuture<BatchNotificationResult>
    
    /**
     * FCM 전용: 배치 처리
     */
    fun sendFcmBatchAsync(
        tokens: List<String>,
        title: String,
        body: String
    ): CompletableFuture<BatchNotificationResult>
}
