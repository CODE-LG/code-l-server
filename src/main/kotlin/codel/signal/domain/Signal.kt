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

    var message : String = "",  // 상대방(승인자) 질문에 대한 답변
    @Enumerated(EnumType.STRING)
    var senderStatus: SignalStatus = SignalStatus.PENDING,

    @Enumerated(EnumType.STRING)
    var receiverStatus: SignalStatus = SignalStatus.PENDING,



    ) : BaseTimeEntity() {
    fun getIdOrThrow(): Long = id ?: throw SignalException(HttpStatus.BAD_REQUEST, "id가 없는 시그널 입니다.")

    fun validateSendable(now: LocalDateTime = LocalDateTime.now()) {
        if (!canSendNewSignal(now)) {
            when (senderStatus) {
                SignalStatus.PENDING, SignalStatus.APPROVED, SignalStatus.PENDING_HIDDEN ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "이미 시그널을 보낸 상대입니다.")

                SignalStatus.REJECTED ->
                    throw SignalException(HttpStatus.BAD_REQUEST, "거절된 상대에게는 7일 후에 다시 시그널을 보낼 수 있습니다.")

                SignalStatus.NONE -> return
            }
        }
    }

    private fun canSendNewSignal(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return when (senderStatus) {
            SignalStatus.PENDING, SignalStatus.APPROVED, SignalStatus.PENDING_HIDDEN -> false
            SignalStatus.REJECTED -> updatedAt.plusDays(7).isBefore(now)
            else -> true
        }
    }

    fun accept() {
        validateChangeAcceptable()
        senderStatus = SignalStatus.APPROVED
        receiverStatus = SignalStatus.APPROVED
    }

    fun reject() {
        validateChangeAcceptable()
        senderStatus = SignalStatus.REJECTED
        receiverStatus = SignalStatus.REJECTED
    }

    private fun validateChangeAcceptable() {
        senderStatus.changeBlockedMessage()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }

    fun hide(memberId: Long) {
        validateAccessRight(memberId)
        if (memberId == fromMember.id) {
            validateSenderHideAcceptable()
            senderStatus = when (senderStatus) {
                SignalStatus.PENDING -> SignalStatus.PENDING_HIDDEN
                else -> throw SignalException(HttpStatus.BAD_REQUEST, "숨김처리가 불가능합니다.")
            }
        } else {
            validateReceiverHideAcceptable()
            receiverStatus = when (receiverStatus) {
                SignalStatus.PENDING -> SignalStatus.PENDING_HIDDEN
                else -> throw SignalException(HttpStatus.BAD_REQUEST, "숨김처리가 불가능합니다.")
            }
        }
    }

    private fun validateAccessRight(memberId: Long) {
        if (memberId != fromMember.id && memberId != toMember.id) {
            throw SignalException(HttpStatus.BAD_REQUEST, "자신과 관련된 시그널이 아닙니다.")
        }
    }

    private fun validateSenderHideAcceptable() {
        senderStatus.canHide()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }

    private fun validateReceiverHideAcceptable() {
        senderStatus.canHide()?.let { msg ->
            throw SignalException(HttpStatus.BAD_REQUEST, msg)
        }
    }
}