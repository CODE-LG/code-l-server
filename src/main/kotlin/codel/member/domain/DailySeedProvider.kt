package codel.member.domain

import java.time.LocalDate
import kotlin.math.absoluteValue

class DailySeedProvider {
    companion object {
        fun generateDailySeedForMember(memberId: Long): Long {
            val today = LocalDate.now()
            val key = "$memberId-$today"
            return key.hashCode().toLong().absoluteValue
        }
    }
}
