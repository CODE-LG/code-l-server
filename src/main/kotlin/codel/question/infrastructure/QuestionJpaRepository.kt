package codel.question.infrastructure

import codel.chat.domain.ChatRoomQuestion
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import codel.question.domain.QuestionGroup
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
        AND q.category IN ('VALUES', 'FAVORITE', 'DATE', 'MEMORY', 'WANT_TALK')
    """)
    fun findActiveQuestionsForSignup(): List<Question>

    /**
     * 채팅방에서 특정 카테고리의 미사용 질문 조회 (그룹별)
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.isActive = true
        AND q.category = :category
        AND q.questionGroup = :questionGroup
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq
            WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = true
        )
    """)
    fun findUnusedQuestionsByChatRoomAndCategoryAndGroup(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("category") category: QuestionCategory,
        @Param("questionGroup") questionGroup: QuestionGroup
    ): List<Question>

    /**
     * 채팅방에서 특정 카테고리의 미사용 질문 조회 (그룹 무관)
     */
    @Query("""
        SELECT q FROM Question q
        WHERE q.isActive = true
        AND q.category = :category
        AND q.id NOT IN (
            SELECT crq.question.id FROM ChatRoomQuestion crq
            WHERE crq.chatRoom.id = :chatRoomId AND crq.isUsed = true
        )
    """)
    fun findUnusedQuestionsByChatRoomAndCategory(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("category") category: QuestionCategory
    ): List<Question>
    
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
     * 관리자: 카테고리/그룹/상태 필터 조회
     */
    @Query("""
        SELECT q FROM Question q
        WHERE (:keyword IS NULL OR :keyword = '' OR q.content LIKE CONCAT('%', :keyword, '%') OR q.description LIKE CONCAT('%', :keyword, '%'))
        AND (:category IS NULL OR q.category = :category)
        AND (:questionGroup IS NULL OR q.questionGroup = :questionGroup)
        AND (:isActive IS NULL OR q.isActive = :isActive)
        ORDER BY q.createdAt DESC
    """)
    fun findAllWithFilterV2(
        @Param("keyword") keyword: String?,
        @Param("category") category: QuestionCategory?,
        @Param("questionGroup") questionGroup: QuestionGroup?,
        @Param("isActive") isActive: Boolean?,
        pageable: Pageable
    ): Page<Question>

    /**
     * 채팅방 질문 통계 - 질문별 사용 횟수 (상위 N개)
     * 초기 질문(isInitial=true) 제외, 질문하기 버튼 클릭으로 추가된 질문만 집계
     */
    @Query("""
        SELECT q.id, q.content, q.category, COUNT(crq) as selectionCount
        FROM ChatRoomQuestion crq
        JOIN crq.question q
        WHERE crq.isInitial = false
        GROUP BY q.id, q.content, q.category
        ORDER BY COUNT(crq) DESC
    """)
    fun findTopSelectedQuestions(pageable: Pageable): List<Array<Any>>

    /**
     * 채팅방 질문 통계 - 날짜 범위 기준 질문별 사용 횟수 (상위 N개)
     * 특정 기간 동안 질문하기 버튼으로 추가된 질문만 집계
     */
    @Query("""
        SELECT q.id, q.content, q.category, COUNT(crq) as selectionCount
        FROM ChatRoomQuestion crq
        JOIN crq.question q
        WHERE crq.isInitial = false
        AND crq.createdAt >= :startDate
        AND crq.createdAt < :endDate
        GROUP BY q.id, q.content, q.category
        ORDER BY COUNT(crq) DESC
    """)
    fun findTopSelectedQuestionsByDateRange(
        @Param("startDate") startDate: java.time.LocalDateTime,
        @Param("endDate") endDate: java.time.LocalDateTime,
        pageable: Pageable
    ): List<Array<Any>>

    /**
     * 활성화된 질문 카테고리별 분포
     * Question 테이블에 등록된 활성 질문들의 카테고리별 개수
     */
    @Query("""
        SELECT q.category, COUNT(q) as count
        FROM Question q
        WHERE q.isActive = true
        GROUP BY q.category
        ORDER BY COUNT(q) DESC
    """)
    fun findQuestionCategoryStats(): List<Array<Any>>
}
