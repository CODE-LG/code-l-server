package codel.kpi.infrastructure

import codel.chat.domain.Chat
import codel.chat.domain.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * KPI 집계 전용 채팅 메시지 Repository
 *
 * 애플리케이션 로직과 분리하여 KPI 집계 성능 최적화 및 관심사 분리
 */
@Repository
interface KpiChatMessageRepository : JpaRepository<Chat, Long> {

    /**
     * 특정 채팅방의 모든 메시지를 시간순으로 조회
     */
    fun findByChatRoomOrderBySentAtAsc(chatRoom: ChatRoom): List<Chat>

    /**
     * 특정 채팅방의 메시지 개수
     */
    fun countByChatRoom(chatRoom: ChatRoom): Long

    /**
     * 채팅방 ID로 메시지 개수 조회
     */
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.chatRoom.id = :chatRoomId")
    fun countByChatRoomId(@Param("chatRoomId") chatRoomId: Long): Long
}
