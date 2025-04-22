package codel.notification.domain

data class Notification(
    val token: String,
    val title: String,
    val body: String,
)
