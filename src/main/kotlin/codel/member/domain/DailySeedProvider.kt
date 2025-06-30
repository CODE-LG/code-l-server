package codel.member.domain

import java.time.LocalDate
import java.util.UUID
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
    }
}
