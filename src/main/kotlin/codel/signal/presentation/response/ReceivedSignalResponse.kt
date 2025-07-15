package codel.signal.presentation.response

import codel.member.presentation.response.MemberProfileResponse
import codel.signal.domain.Signal
import codel.signal.domain.SignalStatus
import java.time.LocalDateTime

data class ReceivedSignalResponse(
    val signalId: Long,
    val fromMember: MemberProfileResponse,
    val status: SignalStatus,
    val createAt: LocalDateTime
) {
    companion object {
        fun from(signal: Signal): ReceivedSignalResponse {
            return ReceivedSignalResponse(
                signalId = signal.id!!,
                fromMember = MemberProfileResponse.toResponse(signal.fromMember),
                status = signal.status,
                createAt = signal.createdAt
            )
        }
    }
}