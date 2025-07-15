package codel.signal.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import codel.signal.exception.SignalException
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

@Entity
@Table(name = "member_signal")
class Signal(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    val fromMember: Member,
    @ManyToOne(fetch = FetchType.LAZY)
    val toMember: Member,
    @Enumerated(EnumType.STRING)
    var status: SignalStatus = SignalStatus.PENDING
) : BaseTimeEntity() {

    fun validateSendable(now: LocalDateTime = LocalDateTime.now()) {
        if (!canSendNewSignal(now)) {
            when (status) {
                SignalStatus.PENDING, SignalStatus.ACCEPTED, SignalStatus.HIDDEN ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "이미 시그널을 보낸 상대입니다.")
                SignalStatus.REJECTED ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "거절된 상대에게는 7일 후에 다시 시그널을 보낼 수 있습니다.")
            }
        }
    }

    fun canSendNewSignal(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return when (status) {
            SignalStatus.PENDING, SignalStatus.ACCEPTED, SignalStatus.HIDDEN -> false
            SignalStatus.REJECTED -> updatedAt.plusDays(7).isBefore(now)
        }
    }

    fun validateChangeAcceptable() {
        status.changeBlockedMessage()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }

    fun accepct() {
        status = SignalStatus.ACCEPTED
    }
}