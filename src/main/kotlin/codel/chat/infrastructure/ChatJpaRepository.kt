package codel.chat.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ChatJpaRepository : JpaRepository<Chat, Long> {
    @Query(
        """
    SELECT c
    FROM Chat c
    WHERE c.fromChatRoomMember.chatRoom = :chatRoom
    """,
    )
    fun findAllByFromChatRoom(
        chatRoom: ChatRoom,
        pageable: Pageable,
    ): Page<Chat>

    @Query(
        """
            SELECT count(c) from Chat c
            WHERE c.fromChatRoomMember.chatRoom = :chatRoom
            AND c.sentAt > :afterTime
        """,
    )
    fun countByChatRoomAfterLastChat(
        chatRoom: ChatRoom,
        afterTime: LocalDateTime,
    ): Int

    @Query("""
    SELECT c
        FROM Chat c
        WHERE c.chatRoom = :chatRoom
        AND c.id >= :lastChatId
        ORDER BY c.id ASC
    """)
    fun findNextChats(
        chatRoom: ChatRoom,
        lastChatId: Long,
        pageable: Pageable
    ): Page<Chat>

    @Query("""
    SELECT c
        FROM Chat c
        WHERE c.chatRoom = :chatRoom
        AND c.id >= :lastChatId
        ORDER BY c.id ASC
    """)
    fun findNextChats(
        chatRoom: ChatRoom,
        pageable: Pageable
    ): Page<Chat>
}
