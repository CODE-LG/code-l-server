package codel.notification.domain.sender

import codel.notification.domain.NotificationType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.OffsetDateTime
import codel.notification.domain.Notification as CodelNotification

@Component
@ConditionalOnProperty(name = ["discord.webhook.url"])
class DiscordNotificationSender(
    private val restTemplate: RestTemplate,
    @Value("\${discord.webhook.url}")
    private val webhookUrl: String,
) : NotificationSender {
    override fun supports(type: NotificationType): Boolean = type == NotificationType.DISCORD || type == NotificationType.ALL

    override fun send(notification: CodelNotification): String {
        val now = OffsetDateTime.now().toString() // ISO 8601 포맷 (Z 포함)

        val embedBody = createEmbedBody(notification, now)

        try {
            restTemplate.postForEntity(webhookUrl, embedBody, String::class.java)
        } catch (e: Exception) {
            throw RuntimeException("디스코드 메시지 전송 실패: ${e.message}", e)
        }
        return "ok"
    }

    private fun createEmbedBody(
        notification: CodelNotification,
        now: String
    ): Map<String, List<Map<String, Any>>> {
        // title에 이모지가 포함되어 있으면 그대로 사용, 아니면 기본 이모지 추가
        val titleWithEmoji = if (notification.title.matches(Regex(".*[\\p{So}\\p{Cn}].*"))) {
            notification.title
        } else {
            "📩 ${notification.title}"
        }

        // body를 필드로 분리할지 description으로 사용할지 결정
        val embedMap = mutableMapOf<String, Any>(
            "title" to titleWithEmoji,
            "description" to notification.body,
            "color" to 3447003, // 파란색 계열
            "footer" to mapOf("text" to "🕒 CODEL 시스템 알림"),
            "timestamp" to now
        )

        return mapOf("embeds" to listOf(embedMap))
    }
}
