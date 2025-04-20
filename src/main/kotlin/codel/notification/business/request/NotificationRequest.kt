package codel.notification.business.request

data class NotificationRequest(
    val token: String,
    val title: String,
    val body: String,
)
