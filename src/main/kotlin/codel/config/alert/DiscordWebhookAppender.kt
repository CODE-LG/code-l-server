package codel.config.alert

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.AppenderBase
import java.net.HttpURLConnection
import java.net.URL
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class DiscordWebhookAppender : AppenderBase<ILoggingEvent>() {
    var webhookUrl: String? = null

    override fun append(eventObject: ILoggingEvent) {
        try {
            val level = eventObject.level.levelStr
            val timestamp = ZonedDateTime.ofInstant(Date(eventObject.timeStamp).toInstant(), ZoneId.of("Asia/Seoul"))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val message = eventObject.formattedMessage.take(1500)
            val mdcMap = eventObject.mdcPropertyMap
            val mdcText = if (mdcMap.isNullOrEmpty()) {
                "없음"
            } else {
                mdcMap.entries.joinToString("\n") { (key, value) -> "$key: $value" }
            }

            val connection = URL(webhookUrl).openConnection() as HttpURLConnection
            val payload = """
                {
                  "embeds": [
                    {
                      "title": "[$level] 로그 발생",
                      "color": ${getColor(level)},
                      "fields": [
                        {
                          "name": "시간",
                          "value": "$timestamp",
                          "inline": false
                        },
                        {
                          "name": "메시지",
                          "value": "${escapeJson(message)}",
                          "inline": false
                        },
                        {
                          "name": "로거",
                          "value": "${eventObject.loggerName}",
                          "inline": false
                        },
                        {
                          "name": "호출 위치",
                          "value": "${escapeJson(eventObject.callerData.firstOrNull()?.toString() ?: "Unknown")}",
                          "inline": false
                        },
                        {
                          "name": "MDC",
                          "value": "${escapeJson(mdcText)}",
                          "inline": false
                        },
                        {
                            "name": "에러",
                            "value": "${escapeJson(eventObject.throwableProxy?.message ?: "없음")}",
                            "inline": false
                        }
                      ]
                    }
                  ]
                }
            """.trimIndent()
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            connection.outputStream.use {
                it.write(payload.toByteArray(Charsets.UTF_8))
            }

            connection.inputStream.close() // fire the request
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun escapeJson(text: String): String =
        text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")

    private fun getColor(level: String): Int {
        return when (level) {
            "ERROR" -> 0xFF0000
            "WARN" -> 0xFFA500
            else -> 0x00BFFF
        }
    }
}
