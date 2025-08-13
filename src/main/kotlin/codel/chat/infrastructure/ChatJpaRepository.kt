package codel.chat.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import codel.member.domain.Member
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
    WHERE c.chatRoom = :chatRoom 
    AND c.senderType != 'SYSTEM'
    AND c.fromChatRoomMember IS NOT NULL
    """,
    )
    fun findAllByFromChatRoom(
        chatRoom: ChatRoom,
        pageable: Pageable,
    ): Page<Chat>

    @Query(
        """
            SELECT count(c) from Chat c
            WHERE c.chatRoom = :chatRoom
            AND c.sentAt > :afterTime
            AND c.fromChatRoomMember.member != :requester
            AND (
                c.senderType = 'USER' 
                OR c.chatContentType IN ('CODE_QUESTION', 'CODE_UNLOCKED_REQUEST')
            )
        """,
    )
    fun countByChatRoomAfterLastChat(
        chatRoom: ChatRoom,
        afterTime: LocalDateTime,
        requester: Member,
    ): Int

    @Query(
        """
        SELECT count(c) from Chat c
        WHERE c.chatRoom = :chatRoom
        AND c.fromChatRoomMember.member != :requester
        AND (
            c.senderType = 'USER' 
            OR c.chatContentType IN ('CODE_QUESTION', 'CODE_UNLOCKED_REQUEST')
        )
    """)
    fun countByChatRoomAfterLastChat(chatRoom: ChatRoom, requester: Member): Int

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
        ORDER BY c.id ASC
    """)
    fun findNextChats(
        chatRoom: ChatRoom,
        pageable: Pageable
    ): Page<Chat>

    @Query("""
    SELECT c
        FROM Chat c
        WHERE c.chatRoom = :chatRoom
        AND c.id <= :lastChatId
        ORDER BY c.id DESC 
    """)
    fun findPrevChats(
        chatRoom: ChatRoom,
        lastChatId: Long,
        pageable: Pageable
    ): Page<Chat>
}
