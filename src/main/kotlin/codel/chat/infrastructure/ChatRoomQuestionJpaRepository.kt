package codel.chat.infrastructure

import codel.chat.domain.ChatRoomQuestion
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface ChatRoomQuestionJpaRepository : JpaRepository<ChatRoomQuestion, Long> {
    
    @Query("SELECT crq FROM ChatRoomQuestion crq WHERE crq.chatRoom.id = :chatRoomId")
    fun findByChatRoomId(@Param("chatRoomId") chatRoomId: Long): List<ChatRoomQuestion>
    
    @Query("SELECT crq FROM ChatRoomQuestion crq WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = false")
    fun findUnusedByChatRoomId(@Param("chatRoomId") chatRoomId: Long): List<ChatRoomQuestion>
    
    @Query("SELECT crq FROM ChatRoomQuestion crq WHERE crq.chatRoom.id = :chatRoomId AND crq.question.id = :questionId")
    fun findByChatRoomIdAndQuestionId(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("questionId") questionId: Long
    ): ChatRoomQuestion?
    
    @Query("SELECT COUNT(crq) FROM ChatRoomQuestion crq WHERE crq.chatRoom.id = :chatRoomId")
    fun countByChatRoomId(@Param("chatRoomId") chatRoomId: Long): Long
}
