package codel.chat.infrastructure

import codel.chat.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface QuestionJpaRepository : JpaRepository<Question, Long> {
    @Query("""
        SELECT q FROM Question q 
        WHERE q.isActive = true 
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq 
            WHERE crq.chatRoom.id = :chatRoomId
        )
        ORDER BY FUNCTION('RAND')
    """)
    fun findUnusedQuestionsByChatRoom(@Param("chatRoomId") chatRoomId: Long): List<Question>
}
