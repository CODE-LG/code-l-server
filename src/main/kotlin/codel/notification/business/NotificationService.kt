package codel.notification.business

import codel.notification.business.request.NotificationRequest
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.springframework.stereotype.Service

@Service
class NotificationService {
    fun sendPushNotification(notificationRequest: NotificationRequest): String {
        val message =
            Message
                .builder()
                .setToken(notificationRequest.token)
                .setNotification(
                    Notification
                        .builder()
                        .setTitle(notificationRequest.title)
                        .setBody(notificationRequest.body)
                        .build(),
                ).build()

        return try {
            FirebaseMessaging.getInstance().send(message)
        } catch (e: FirebaseMessagingException) {
            throw IllegalArgumentException("알림 전송중 오류가 발생했습니다.")
        }
    }
}
