package codel.notification.domain.sender

import codel.config.Loggable
import codel.notification.domain.NotificationType
import codel.notification.exception.NotificationException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import codel.notification.domain.Notification as CodelNotification

@Component
class FcmNotificationSender : NotificationSender, Loggable {
    override fun supports(type: NotificationType): Boolean = type == NotificationType.MOBILE

    override fun send(notification: CodelNotification): String {
        val message =
            Message
                .builder()
                .setToken(notification.targetId)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(notification.title)
                        .setBody(notification.body)
                        .build(),
                ).build()

        return try {
            val response = FirebaseMessaging.getInstance().send(message)
            log.debug { "FCM 메시지 전송 성공: messageId=$response" }
            response
        } catch (e: FirebaseMessagingException) {
            handleFcmError(e, notification.targetId)
            throw NotificationException(HttpStatus.BAD_GATEWAY, "알림 전송중 오류가 발생했습니다: ${e.messagingErrorCode}")
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
