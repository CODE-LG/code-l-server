package codel.chat.domain

class ChatRoom(
    val id: Long? = null,
    val isActive: Boolean = true,
    val lastMessage: String = "",
    val unreadMessagesCount: Int = 0,
) {
    fun getIdOrThrow(): Long = id ?: throw IllegalStateException("채팅방 아이디가 없습니다.")
}
