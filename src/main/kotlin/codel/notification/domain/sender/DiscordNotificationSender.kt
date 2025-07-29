package codel.notification.domain.sender

import codel.notification.domain.NotificationType
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
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
        val embedBody = mapOf(
            "embeds" to listOf(
                mapOf(
                    "title" to "📩 [회원가입 요청]",
                    "description" to "**새로운 사용자가 가입을 요청했습니다.**",
                    "color" to 3447003, // 파란색 계열
                    "fields" to listOf(
                        mapOf(
                            "name" to "닉네임",
                            "value" to notification.body
                        ),
                        mapOf(
                            "name" to "가입 시각",
                            "value" to LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        ),
                        mapOf(
                            "name" to "상태",
                            "value" to "PENDING"
                        )
                    ),
                    "footer" to mapOf("text" to "🕒 CODEL 시스템 알림"),
                    "timestamp" to now
                )
            )
        )
        return embedBody
    }
}
