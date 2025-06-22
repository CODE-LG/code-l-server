package codel.chat.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ChatJpaRepository : JpaRepository<Chat, Long> {
    @Query(
        """
        SELECT c FROM Chat c
        WHERE c.from.chatRoom = :chatRoom
        ORDER BY c.sentAt
    """,
    )
    fun findByFromChatRoomOrderBySentAt(chatRoom: ChatRoom): List<Chat>
}
