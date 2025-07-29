package codel.notification.domain.sender

import codel.notification.domain.NotificationType
import codel.notification.exception.NotificationException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import codel.notification.domain.Notification as CodelNotification

@Component
class FcmNotificationSender : NotificationSender {
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
            FirebaseMessaging.getInstance().send(message)
        } catch (e: FirebaseMessagingException) {
            throw NotificationException(HttpStatus.BAD_GATEWAY, "알림 전송중 오류가 발생했습니다.")
        }
    }
}
