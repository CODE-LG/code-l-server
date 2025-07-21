package codel.signal.domain

import codel.common.domain.BaseTimeEntity
import codel.member.domain.Member
import codel.signal.exception.SignalException
import jakarta.persistence.*
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
                SignalStatus.PENDING, SignalStatus.APPROVED, SignalStatus.PENDING_HIDDEN, SignalStatus.APPROVED_HIDDEN ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "이미 시그널을 보낸 상대입니다.")

                SignalStatus.REJECTED ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "거절된 상대에게는 7일 후에 다시 시그널을 보낼 수 있습니다.")
            }
        }
    }

    private fun canSendNewSignal(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return when (status) {
            SignalStatus.PENDING, SignalStatus.APPROVED, SignalStatus.PENDING_HIDDEN, SignalStatus.APPROVED_HIDDEN -> false
            SignalStatus.REJECTED -> updatedAt.plusDays(7).isBefore(now)
        }
    }

    fun accept() {
        validateChangeAcceptable()
        status = SignalStatus.APPROVED
    }

    fun reject() {
        validateChangeAcceptable()
        status = SignalStatus.REJECTED
    }

    private fun validateChangeAcceptable() {
        status.changeBlockedMessage()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }

    fun hide(memberId: Long) {
        validateHidable(memberId)
        status = when (status) {
            SignalStatus.APPROVED -> SignalStatus.APPROVED_HIDDEN
            SignalStatus.PENDING -> SignalStatus.PENDING_HIDDEN
            else -> status  // 변경 없는 상태 유지
        }
    }

    private fun validateHidable(memberId: Long) {
        validateAccessRight(memberId)
        validateHideAcceptable()
    }

    private fun validateAccessRight(memberId: Long) {
        if (memberId != fromMember.id && memberId != toMember.id) {
            throw SignalException(HttpStatus.BAD_REQUEST, "자신과 관련된 시그널이 아닙니다.")
        }
    }

    private fun validateHideAcceptable() {
        status.canHide()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }
}