package codel.signal.presentation.request

data class SendSignalRequest(
    val toMemberId: Long,
    val message: String,  // 상대방(승인자) 질문에 대한 답변
)