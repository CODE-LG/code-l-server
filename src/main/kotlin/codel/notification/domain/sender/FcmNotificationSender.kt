package codel.notification.domain.sender

import codel.config.Loggable
import codel.notification.domain.NotificationType
import codel.notification.exception.NotificationException
import com.google.firebase.messaging.*
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import codel.notification.domain.Notification as CodelNotification

@Component
class FcmNotificationSender : NotificationSender, Loggable {
    override fun supports(type: NotificationType): Boolean = type == NotificationType.MOBILE

    override fun send(notification: CodelNotification): String {
        val messageBuilder = Message
            .builder()
            .setToken(notification.targetId)
            .setNotification(
                Notification
                    .builder()
                    .setTitle(notification.title)
                    .setBody(notification.body)
                    .build(),
            )

        // data 필드가 있으면 추가
        notification.data?.let { data ->
            messageBuilder.putAllData(data)
        }

        val message = messageBuilder.build()

        return try {
            val response = FirebaseMessaging.getInstance().send(message)
            log.debug { "FCM 메시지 전송 성공: messageId=$response" }
            response
        } catch (e: FirebaseMessagingException) {
            handleFcmError(e, notification.targetId)
            throw NotificationException(HttpStatus.BAD_GATEWAY, "알림 전송중 오류가 발생했습니다: ${e.messagingErrorCode}")
        }
    }
    
    /**
     * FCM 배치 전송 (최대 500개)
     * 
     * @param notifications 전송할 알림 리스트 (최대 500개)
     * @return BatchResponse
     */
    fun sendBatch(notifications: List<CodelNotification>): BatchResponse {
        require(notifications.size <= 500) { "FCM 배치는 최대 500개까지만 가능합니다" }

        val messages = notifications.map { notification ->
            val messageBuilder = Message
                .builder()
                .setToken(notification.targetId)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build(),
                )

            // data 필드가 있으면 추가
            notification.data?.let { data ->
                messageBuilder.putAllData(data)
            }

            messageBuilder.build()
        }
        
        return try {
            val batchResponse = FirebaseMessaging.getInstance().sendAll(messages)
            
            log.info { 
                "📦 FCM 배치 전송 완료 - " +
                "성공: ${batchResponse.successCount}, " +
                "실패: ${batchResponse.failureCount}"
            }
            
            // 실패한 항목 로깅
            batchResponse.responses.forEachIndexed { index, response ->
                if (!response.isSuccessful) {
                    val token = notifications[index].targetId
                    log.warn { "❌ FCM 배치 전송 실패 [${index}] - token=$token, error=${response.exception?.message}" }
                    
                    if (response.exception is FirebaseMessagingException) {
                        handleFcmError(response.exception as FirebaseMessagingException, token)
                    }
                }
            }
            
            batchResponse
        } catch (e: FirebaseMessagingException) {
            log.error(e) { "🔴 FCM 배치 전송 중 오류 발생" }
            throw NotificationException(HttpStatus.BAD_GATEWAY, "배치 알림 전송중 오류가 발생했습니다: ${e.messagingErrorCode}")
        }
    }
    
    private fun handleFcmError(e: FirebaseMessagingException, token: String?) {
        when (e.messagingErrorCode) {
            MessagingErrorCode.INVALID_ARGUMENT -> {
                log.warn { "🔴 FCM 잘못된 토큰: token=$token" }
                // TODO: 토큰 무효화 처리 필요
            }
            MessagingErrorCode.UNREGISTERED -> {
                log.warn { "🔴 FCM 등록되지 않은 토큰 (앱 삭제됨): token=$token" }
                // TODO: 토큰 삭제 처리 필요
            }
            MessagingErrorCode.SENDER_ID_MISMATCH -> {
                log.error { "🔴 FCM Sender ID 불일치: token=$token" }
            }
            MessagingErrorCode.QUOTA_EXCEEDED -> {
                log.error { "🔴 FCM 할당량 초과! 즉시 확인 필요!" }
            }
            MessagingErrorCode.UNAVAILABLE -> {
                log.warn { "⚠️ FCM 서버 일시적 장애: token=$token" }
                // TODO: 재시도 로직 고려
            }
            MessagingErrorCode.INTERNAL -> {
                log.error(e) { "🔴 FCM 내부 오류: token=$token" }
            }
            MessagingErrorCode.THIRD_PARTY_AUTH_ERROR -> {
                log.error { "🔴 FCM 인증 오류: Firebase 설정 확인 필요" }
            }
            else -> {
                log.error(e) { "🔴 FCM 알 수 없는 오류: errorCode=${e.messagingErrorCode}, token=$token" }
            }
        }
    }
}
