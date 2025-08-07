package codel.member.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.math.absoluteValue

class DailySeedProvider {
    companion object {
        fun generateDailySeedForMember(memberId: Long): Long {
            val today = LocalDate.now()
            val key = "$memberId-$today"
            return key.hashCode().toLong().absoluteValue
        }

        fun generateRandomSeed(): Long {
            val uuid = UUID.randomUUID()
            return (uuid.mostSignificantBits xor uuid.leastSignificantBits).absoluteValue
        }

        fun generateMemberSeedEveryTenHours
                    (memberId: Long) : Long{
            val now = LocalDateTime.now()
            val baseDate = if (now.hour < 10) {
                // 아직 오전 10시 전이면 어제 날짜로 간주
                now.toLocalDate().minusDays(1)
            } else {
                // 오전 10시 이후면 오늘 날짜
                now.toLocalDate()
            }
            val isDayBlock = now.hour in 10..21 // 10:00 ~ 21:59

            val key = "$memberId-$baseDate-BLOCK-${if (isDayBlock) "DAY" else "NIGHT"}"
            return key.hashCode().toLong().absoluteValue
        }
    }
}
