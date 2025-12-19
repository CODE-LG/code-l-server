package codel.notification.business

import codel.notification.domain.NotificationType
import codel.notification.domain.sender.NotificationSender
import org.springframework.stereotype.Service
import codel.notification.domain.Notification as CodelNotification

@Service
class NotificationService(
    val senders: List<NotificationSender>,
) {
    fun send(notification: CodelNotification) {
        val matchingSenders =
            if (notification.type == NotificationType.ALL) {
                senders
            } else {
                senders.filter { it.supports(notification.type) }
            }

        if (matchingSenders.isEmpty()) {
            throw IllegalArgumentException("지원하지 않는 알림 타입입니다: ${notification.type}")
        }

        matchingSenders.forEach { sender ->
            sender.send(notification)
        }
    }
}
