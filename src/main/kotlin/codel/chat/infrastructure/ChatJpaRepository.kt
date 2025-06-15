package codel.chat.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChatJpaRepository : JpaRepository<Chat, Long> {
    fun findByChatRoomOrderBySentAt(chatRoom: ChatRoom): List<Chat>
}
