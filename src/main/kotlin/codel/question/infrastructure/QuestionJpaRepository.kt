package codel.question.infrastructure

import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface QuestionJpaRepository : JpaRepository<Question, Long> {
    
    @Query("SELECT q FROM Question q WHERE q.isActive = true")
    fun findActiveQuestions(): List<Question>
    
    @Query("""
        SELECT q FROM Question q 
        WHERE q.isActive = true 
        AND q.category NOT IN ('IF', 'BALANCE_ONE')
    """)
    fun findActiveQuestionsForSignup(): List<Question>
    
    @Query("""
        SELECT q FROM Question q 
        WHERE q.isActive = true 
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq 
            WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = true
        )
    """)
    fun findUnusedQuestionsByChatRoom(@Param("chatRoomId") chatRoomId: Long): List<Question>
    
    @Query("""
        SELECT q FROM Question q
        WHERE (:keyword IS NULL OR :keyword = '' OR q.content LIKE CONCAT('%', :keyword, '%') OR q.description LIKE CONCAT('%', :keyword, '%'))
        AND (:category IS NULL OR q.category = :category)
        AND (:isActive IS NULL OR q.isActive = :isActive)
        ORDER BY q.createdAt DESC
    """)
    fun findAllWithFilter(
        @Param("keyword") keyword: String?,
        @Param("category") category: QuestionCategory?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable
    ): Page<Question>

    /**
     * 프로필 대표 질문 통계 - 질문별 선택 횟수 (상위 N개)
     */
    @Query("""
        SELECT q.id, q.content, q.category, COUNT(p) as selectionCount
        FROM Profile p
        JOIN p.representativeQuestion q
        WHERE q IS NOT NULL
        GROUP BY q.id, q.content, q.category
        ORDER BY COUNT(p) DESC
    """)
    fun findTopSelectedQuestions(pageable: Pageable): List<Array<Any>>

    /**
     * 프로필 대표 질문 카테고리별 통계
     */
    @Query("""
        SELECT q.category, COUNT(p) as count
        FROM Profile p
        JOIN p.representativeQuestion q
        WHERE q IS NOT NULL
        GROUP BY q.category
        ORDER BY COUNT(p) DESC
    """)
    fun findQuestionCategoryStats(): List<Array<Any>>
}
