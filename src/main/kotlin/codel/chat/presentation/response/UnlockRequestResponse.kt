package codel.chat.presentation.response

import codel.chat.domain.CodeUnlockRequest
import codel.chat.domain.UnlockRequestStatus
import java.time.LocalDateTime

data class UnlockRequestResponse(
    val requestId: Long,
    val requesterId: Long,
    val requesterName: String,
    val status: UnlockRequestStatus,
    val requestedAt: LocalDateTime,
    val processedAt: LocalDateTime?
) {
    companion object {
        fun from(request: CodeUnlockRequest): UnlockRequestResponse {
            return UnlockRequestResponse(
                requestId = request.getIdOrThrow(),
                requesterId = request.requester.getIdOrThrow(),
                requesterName = request.requester.getProfileOrThrow().getCodeNameOrThrow(),
                status = request.status,
                requestedAt = request.requestedAt,
                processedAt = request.processedAt
            )
        }
    }
}
