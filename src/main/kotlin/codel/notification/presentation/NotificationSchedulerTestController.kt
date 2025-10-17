package codel.notification.presentation

import codel.notification.business.MatchingNotificationScheduler
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 스케줄러 테스트용 컨트롤러
 * dev, local 프로파일에서만 활성화됨
 */
@RestController
@RequestMapping("/api/v1/test/scheduler")
@Profile("dev", "local")
@Tag(name = "스케줄러 테스트 API (개발용)")
class NotificationSchedulerTestController(
    private val matchingNotificationScheduler: MatchingNotificationScheduler
) {

    @PostMapping("/matching/morning")
    @Operation(summary = "오전 매칭 알림 즉시 전송", description = "스케줄러를 기다리지 않고 즉시 오전 매칭 알림을 전송합니다.")
    fun testMorningNotification(): TestSchedulerResponse {
        matchingNotificationScheduler.sendMorningMatchingNotification()
        return TestSchedulerResponse(
            success = true,
            message = "오전 매칭 알림 전송 완료. 로그를 확인하세요."
        )
    }

    @PostMapping("/matching/evening")
    @Operation(summary = "오후 매칭 알림 즉시 전송", description = "스케줄러를 기다리지 않고 즉시 오후 매칭 알림을 전송합니다.")
    fun testEveningNotification(): TestSchedulerResponse {
        matchingNotificationScheduler.sendEveningMatchingNotification()
        return TestSchedulerResponse(
            success = true,
            message = "오후 매칭 알림 전송 완료. 로그를 확인하세요."
        )
    }
}

data class TestSchedulerResponse(
    val success: Boolean,
    val message: String
)
