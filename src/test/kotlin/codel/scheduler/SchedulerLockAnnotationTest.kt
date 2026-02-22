package codel.scheduler

import codel.kpi.batch.KpiScheduler
import codel.notification.business.MatchingNotificationScheduler
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import org.junit.jupiter.api.Test
import org.springframework.scheduling.annotation.Scheduled
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * SchedulerLock 어노테이션 검증 테스트
 *
 * 목적:
 * - 모든 @Scheduled 메서드에 @SchedulerLock이 빠짐없이 적용되었는지 검증
 * - 다중 서버 환경에서 @SchedulerLock 누락 시 중복 실행되므로 CI에서 자동 검증 필요
 *
 * 검증 항목:
 * 1. 모든 @Scheduled 메서드에 @SchedulerLock 적용
 * 2. @SchedulerLock의 name이 각각 고유함 (중복 name은 ShedLock 충돌 유발)
 * 3. lockAtLeastFor가 설정되어 있음 (빈 값이면 다중 서버에서 중복 실행 가능)
 *
 * Mock 없음:
 * - Java Reflection API로 클래스의 메서드 어노테이션을 직접 스캔
 */
class SchedulerLockAnnotationTest {

    private val schedulerClasses = listOf(
        KpiScheduler::class,
        MatchingNotificationScheduler::class
    )

    @Test
    fun `모든 @Scheduled 메서드에 @SchedulerLock이 적용되어 있다`() {
        schedulerClasses.forEach { kClass ->
            val scheduledMethods = kClass.declaredMemberFunctions.filter { method ->
                method.findAnnotation<Scheduled>() != null
            }

            assertTrue(scheduledMethods.isNotEmpty(), "${kClass.simpleName}에 @Scheduled 메서드가 없습니다.")

            scheduledMethods.forEach { method ->
                val schedulerLock = method.findAnnotation<SchedulerLock>()
                assertNotNull(
                    schedulerLock,
                    "${kClass.simpleName}.${method.name}에 @SchedulerLock이 누락되었습니다."
                )

                println("✓ ${kClass.simpleName}.${method.name} → @SchedulerLock(name=\"${schedulerLock.name}\")")
            }
        }
    }

    @Test
    fun `@SchedulerLock의 name이 각각 고유하다`() {
        val allLockNames = mutableListOf<String>()

        schedulerClasses.forEach { kClass ->
            val scheduledMethods = kClass.declaredMemberFunctions.filter { method ->
                method.findAnnotation<Scheduled>() != null
            }

            scheduledMethods.forEach { method ->
                val schedulerLock = method.findAnnotation<SchedulerLock>()
                assertNotNull(schedulerLock, "${kClass.simpleName}.${method.name}에 @SchedulerLock이 누락되었습니다.")
                allLockNames.add(schedulerLock.name)
            }
        }

        val duplicates = allLockNames.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue(
            duplicates.isEmpty(),
            "중복된 @SchedulerLock name이 발견되었습니다: ${duplicates.keys}. " +
                    "각 스케줄러의 name은 고유해야 합니다."
        )

        println("✓ 모든 @SchedulerLock name이 고유합니다: $allLockNames")
    }

    @Test
    fun `@SchedulerLock의 lockAtLeastFor가 설정되어 있다`() {
        schedulerClasses.forEach { kClass ->
            val scheduledMethods = kClass.declaredMemberFunctions.filter { method ->
                method.findAnnotation<Scheduled>() != null
            }

            scheduledMethods.forEach { method ->
                val schedulerLock = method.findAnnotation<SchedulerLock>()
                assertNotNull(schedulerLock, "${kClass.simpleName}.${method.name}에 @SchedulerLock이 누락되었습니다.")

                assertTrue(
                    schedulerLock.lockAtLeastFor.isNotBlank(),
                    "${kClass.simpleName}.${method.name}의 @SchedulerLock.lockAtLeastFor가 비어있습니다. " +
                            "최소 락 시간을 설정해야 합니다 (예: PT5M)."
                )

                println("✓ ${kClass.simpleName}.${method.name} → lockAtLeastFor=\"${schedulerLock.lockAtLeastFor}\"")
            }
        }
    }

    @Test
    fun `@SchedulerLock의 lockAtMostFor가 설정되어 있다`() {
        schedulerClasses.forEach { kClass ->
            val scheduledMethods = kClass.declaredMemberFunctions.filter { method ->
                method.findAnnotation<Scheduled>() != null
            }

            scheduledMethods.forEach { method ->
                val schedulerLock = method.findAnnotation<SchedulerLock>()
                assertNotNull(schedulerLock, "${kClass.simpleName}.${method.name}에 @SchedulerLock이 누락되었습니다.")

                assertTrue(
                    schedulerLock.lockAtMostFor.isNotBlank(),
                    "${kClass.simpleName}.${method.name}의 @SchedulerLock.lockAtMostFor가 비어있습니다. " +
                            "최대 락 시간을 설정해야 합니다 (예: PT30M)."
                )

                println("✓ ${kClass.simpleName}.${method.name} → lockAtMostFor=\"${schedulerLock.lockAtMostFor}\"")
            }
        }
    }

    @Test
    fun `KpiScheduler의 @Scheduled 메서드 목록을 출력한다`() {
        val kpiScheduler = KpiScheduler::class
        val scheduledMethods = kpiScheduler.declaredMemberFunctions.filter { method ->
            method.findAnnotation<Scheduled>() != null
        }

        println("=== KpiScheduler @Scheduled 메서드 목록 ===")
        scheduledMethods.forEach { method ->
            val scheduled = method.findAnnotation<Scheduled>()
            val schedulerLock = method.findAnnotation<SchedulerLock>()
            println("메서드: ${method.name}")
            println("  @Scheduled cron: ${scheduled?.cron}")
            println("  @SchedulerLock name: ${schedulerLock?.name}")
            println("  @SchedulerLock lockAtLeastFor: ${schedulerLock?.lockAtLeastFor}")
            println("  @SchedulerLock lockAtMostFor: ${schedulerLock?.lockAtMostFor}")
            println()
        }
    }

    @Test
    fun `MatchingNotificationScheduler의 @Scheduled 메서드 목록을 출력한다`() {
        val matchingScheduler = MatchingNotificationScheduler::class
        val scheduledMethods = matchingScheduler.declaredMemberFunctions.filter { method ->
            method.findAnnotation<Scheduled>() != null
        }

        println("=== MatchingNotificationScheduler @Scheduled 메서드 목록 ===")
        scheduledMethods.forEach { method ->
            val scheduled = method.findAnnotation<Scheduled>()
            val schedulerLock = method.findAnnotation<SchedulerLock>()
            println("메서드: ${method.name}")
            println("  @Scheduled cron: ${scheduled?.cron}")
            println("  @SchedulerLock name: ${schedulerLock?.name}")
            println("  @SchedulerLock lockAtLeastFor: ${schedulerLock?.lockAtLeastFor}")
            println("  @SchedulerLock lockAtMostFor: ${schedulerLock?.lockAtMostFor}")
            println()
        }
    }
}
