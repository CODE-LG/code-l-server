package codel.notification.domain

data class Notification(
    val type: NotificationType,
    val targetId: String?, // 사용자 ID or null (예: Discord)
    val title: String,
    val body: String,
    val data: Map<String, String>? = null, // FCM data payload (chatRoomId, senderId, type 등)
)
