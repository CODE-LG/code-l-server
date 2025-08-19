package codel.chat.presentation.response

import codel.chat.business.UnlockInfo

data class UnlockInfoResponse(
    val isUnlocked: Boolean,
    val currentRequest: UnlockRequestResponse?,
    val canRequest: Boolean
) {
    companion object {
        fun from(unlockInfo: UnlockInfo): UnlockInfoResponse {
            return UnlockInfoResponse(
                isUnlocked = unlockInfo.isUnlocked,
                currentRequest = unlockInfo.currentRequest?.let { UnlockRequestResponse.from(it) },
                canRequest = unlockInfo.canRequest
            )
        }
    }
}
