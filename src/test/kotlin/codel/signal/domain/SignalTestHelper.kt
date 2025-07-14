package codel.signal.domain

import java.time.LocalDateTime

object SignalTestHelper {
    fun setCreatedAt(signal: Signal, createdAt: LocalDateTime) {
        val field = signal.javaClass.superclass.getDeclaredField("createdAt")
        field.isAccessible = true
        field.set(signal, createdAt)
    }

    fun setUpdatedAt(signal: Signal, updatedAt: LocalDateTime) {
        val field = signal.javaClass.superclass.getDeclaredField("updatedAt")
        field.isAccessible = true
        field.set(signal, updatedAt)
    }
} 