package codel.question.infrastructure

import codel.question.domain.Question
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface QuestionJpaRepository : JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.isActive = true ORDER BY q.priority DESC, FUNCTION('RAND')")
    fun findActiveQuestions(): List<Question>
    
    @Query("SELECT q FROM Question q WHERE q.category = :category AND q.isActive = true ORDER BY q.priority DESC")
    fun findActiveQuestionsByCategory(@Param("category") category: String): List<Question>
    
    @Query("""
        SELECT q FROM Question q 
        WHERE q.isActive = true 
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq 
            WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = true
        )
        ORDER BY q.priority DESC, FUNCTION('RAND')
    """)
    fun findUnusedQuestionsByChatRoom(@Param("chatRoomId") chatRoomId: Long): List<Question>
    
    @Query("""
        SELECT q FROM Question q 
        WHERE q.isActive = true
        AND q.category = :category
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq 
            WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = true
        )
        ORDER BY q.priority DESC, FUNCTION('RAND')
    """)
    fun findUnusedQuestionsByChatRoomAndCategory(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("category") category: String
    ): List<Question>
}
