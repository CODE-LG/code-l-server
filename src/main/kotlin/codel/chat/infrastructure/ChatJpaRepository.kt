package codel.chat.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChatJpaRepository : JpaRepository<Chat, Long> {
    fun findByFromChatRoomOrderBySentAt(chatRoom: ChatRoom): List<Chat>

    @Query(
        """
        SELECT count(c) from Chat c
        WHERE c.from.chatRoom = :chatRoom
        AND c.sentAt > :afterTime
        """,
    )
    fun countByChatRoomAfterLastChat(
        chatRoom: ChatRoom,
        afterTime: LocalDateTime,
    ): Int

    @Query(
        """
        SELECT *
        FROM chat c
        INNER JOIN (
            SELECT chat_room_id, MAX(sent_at) as max_sent_at
            FROM chat
            GROUP BY chat_room_id
        ) latest
        ON c.chat_room_id = latest.chat_room_id AND c.sent_at = latest.max_sent_at
        WHERE c.chat_room_id IN (:chatRoomIds)
        """,
        nativeQuery = true,
    )
    fun findRecentChatByChatRooms(chatRoom: List<ChatRoom>): List<Chat>
}
