package codel.signal.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "member_signal")
class Signal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne
    val fromMember: Member,
    @ManyToOne
    val toMember: Member,
    @Enumerated(EnumType.STRING)
    var status: SignalStatus = SignalStatus.PENDING,
    override var createdAt: LocalDateTime = LocalDateTime.now()
) : BaseTimeEntity() {
    // 테스트 전용 생성자
    companion object {
        // 테스트 전용 팩토리
        fun testInstance(
            id: Long? = null,
            fromMember: Member,
            toMember: Member,
            status: SignalStatus = SignalStatus.PENDING,
            createdAt: LocalDateTime
        ): Signal {
            return Signal(id, fromMember, toMember, status, createdAt)
        }
    }

    fun canSendNewSignal(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return when (status) {
            SignalStatus.PENDING, SignalStatus.ACCEPTED -> false
            SignalStatus.REJECTED -> createdAt.plusDays(15).isBefore(now)
        }
    }

    fun validateSendable(now: LocalDateTime = LocalDateTime.now()) {
        if (!canSendNewSignal(now)) {
            when (status) {
                SignalStatus.PENDING, SignalStatus.ACCEPTED ->
                    throw codel.signal.exception.SignalException(org.springframework.http.HttpStatus.BAD_REQUEST, "이미 시그널을 보낸 상대입니다.")
                SignalStatus.REJECTED ->
                    throw codel.signal.exception.SignalException(org.springframework.http.HttpStatus.BAD_REQUEST, "거절된 상대에게는 15일 후에 다시 시그널을 보낼 수 있습니다.")
            }
        }
    }
}