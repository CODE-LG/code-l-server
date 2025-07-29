package codel.notification.domain.sender

import codel.notification.domain.Notification
import codel.notification.domain.NotificationType

interface NotificationSender {
    fun supports(type: NotificationType): Boolean

    fun send(notification: Notification): String
}
