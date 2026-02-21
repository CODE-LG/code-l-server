package codel.notification.business

import codel.config.Loggable
import codel.member.domain.MemberRepository
import codel.member.domain.MemberStatus
import codel.notification.domain.Notification
import codel.notification.domain.NotificationType
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class MatchingNotificationScheduler(
    private val memberRepository: MemberRepository,
    private val asyncNotificationService: IAsyncNotificationService,
    private val notificationService: NotificationService
) : Loggable {

    /**
     * 매일 오전 10시에 실행
     * cron: 초 분 시 일 월 요일
     */
    @Scheduled(cron = "0 0 10 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "notification_morning", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional(readOnly = true)
    fun sendMorningMatchingNotification() {
        log.info { "🌅 오전 10시 매칭 알림 전송 시작" }
        sendMatchingNotificationToAllUsersAsync("morning")
    }

    /**
     * 매일 오후 10시에 실행
     */
    @Scheduled(cron = "0 0 22 * * *", zone = "Asia/Seoul")
    @SchedulerLock(name = "notification_evening", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    @Transactional(readOnly = true)
    fun sendEveningMatchingNotification() {
        log.info { "🌙 오후 10시 매칭 알림 전송 시작" }
        sendMatchingNotificationToAllUsersAsync("evening")
    }

    /**
     * 비동기 배치 처리로 알림 전송 (개선 버전)
     */
    private fun sendMatchingNotificationToAllUsersAsync(timeSlot: String) {
        val startTime = System.currentTimeMillis()

        // DONE 상태의 활성 회원만 조회
        val activeMembers = memberRepository.findByMemberStatus(MemberStatus.DONE)

        if (activeMembers.isEmpty()) {
            log.info { "⚠️ 알림 전송 대상이 없습니다." }
            sendDiscordNotification(timeSlot, 0, 0, 0, 0, 0)
            return
        }

        log.info { "📊 알림 전송 대상: ${activeMembers.size}명" }

        // FCM 토큰이 있는 회원만 필터링
        val membersWithToken = activeMembers.filter { it.fcmToken != null }
        val noTokenCount = activeMembers.size - membersWithToken.size

        if (membersWithToken.isEmpty()) {
            log.warn { "⚠️ FCM 토큰이 있는 회원이 없습니다." }
            sendDiscordNotification(timeSlot, activeMembers.size, 0, 0, noTokenCount, 0)
            return
        }

        log.info { "✅ FCM 토큰 보유: ${membersWithToken.size}명, 토큰 없음: ${noTokenCount}명" }

        // 알림 생성
        val title = "새로운 인연을 만나보세요 💝"
        val body = "지금 새로운 프로필이 업데이트되었어요! 오늘의 인연을 확인해보세요."

        val tokens = membersWithToken.mapNotNull { it.fcmToken }

        // 비동기 배치 전송
        val resultFuture = asyncNotificationService.sendFcmBatchAsync(tokens, title, body)

        // 결과 대기
        val result = try {
            resultFuture.get() // CompletableFuture 완료 대기
        } catch (e: Exception) {
            log.error(e) { "❌ 비동기 알림 전송 중 오류 발생" }
            sendDiscordNotification(timeSlot, activeMembers.size, 0, membersWithToken.size, noTokenCount, 0)
            return
        }

        val duration = System.currentTimeMillis() - startTime

        log.info {
            """
            ✅ $timeSlot 매칭 알림 전송 완료 (${duration}ms)
            - 총 대상: ${activeMembers.size}명
            - 성공: ${result.success}명
            - 실패: ${result.failure}명
            - 토큰 없음: ${noTokenCount}명
            - 성공률: ${String.format("%.1f%%", result.successRate)}
            - 평균 처리: ${if (result.total > 0) result.durationMs / result.total else 0}ms/명
            """.trimIndent()
        }

        // 실패한 경우 상세 로그
        if (result.failure > 0) {
            val errorSummary = result.getErrorSummary()
            log.warn {
                """
                ⚠️ 실패 상세 정보:
                ${errorSummary.entries.joinToString("\n") { "  - ${it.key}: ${it.value}건" }}
                """.trimIndent()
            }
        }

        // 디스코드로 결과 전송
        sendDiscordNotification(
            timeSlot = timeSlot,
            totalCount = activeMembers.size,
            successCount = result.success,
            failCount = result.failure,
            noTokenCount = noTokenCount,
            duration = duration
        )
    }

    private fun sendDiscordNotification(
        timeSlot: String,
        totalCount: Int,
        successCount: Int,
        failCount: Int,
        noTokenCount: Int,
        duration: Long
    ) {
        try {
            val timeSlotKorean = when(timeSlot) {
                "morning" -> "🌅 오전 10시"
                "evening" -> "🌙 오후 10시"
                else -> "🧪 테스트"
            }

            val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            val successRate = if (totalCount > 0) {
                String.format("%.1f%%", (successCount.toDouble() / totalCount) * 100)
            } else {
                "0.0%"
            }

            val statusEmoji = when {
                totalCount == 0 -> "⚠️"
                failCount == 0 -> "✅"
                failCount < totalCount * 0.1 -> "⚡"
                else -> "⚠️"
            }

            val notification = Notification(
                type = NotificationType.DISCORD,
                targetId = null,
                title = "$statusEmoji 매칭 알림 전송 완료 (비동기)",
                body = """
                    **$timeSlotKorean 매칭 알림 전송 결과**
                    
                    📊 **전송 통계**
                    • 총 대상: **${totalCount}명**
                    • 성공: **${successCount}명** ($successRate)
                    • 실패: **${failCount}명**
                    • 토큰 없음: **${noTokenCount}명**
                    
                    ⏱️ **처리 시간**
                    • 소요 시간: **${duration}ms** (${String.format("%.2f", duration / 1000.0)}초)
                    • 평균 처리: **${if (totalCount > 0) duration / totalCount else 0}ms/명**
                    
                    🚀 **성능 개선**
                    • 비동기 배치 처리 적용
                    • FCM 배치 API 활용 (최대 500개/배치)
                    
                    🕐 **실행 시각**
                    • $currentTime (KST)
                """.trimIndent()
            )

            notificationService.send(notification)
            log.info { "✅ 디스코드 알림 전송 완료" }
        } catch (e: Exception) {
            log.warn(e) { "❌ 디스코드 알림 전송 실패" }
        }
    }

    /**
     * 테스트용 - 매 1분마다 실행 (개발/테스트용)
     * 프로덕션에서는 제거하거나 주석 처리
     */
//    @Scheduled(cron = "0 */1 * * * *", zone = "Asia/Seoul")
//    @Transactional(readOnly = true)
//    fun sendTestNotification() {
//        log.info { "🧪 테스트 알림 전송 시작 (1분마다)" }
//        sendMatchingNotificationToAllUsersAsync("test")
//    }
}
