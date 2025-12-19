package codel.question.business

import codel.question.infrastructure.QuestionJpaRepository
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import codel.chat.domain.ChatRoomQuestion
import codel.chat.infrastructure.ChatRoomQuestionJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.member.domain.Member
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class QuestionService(
    private val questionJpaRepository: QuestionJpaRepository,
    private val chatRoomQuestionJpaRepository: ChatRoomQuestionJpaRepository,
    private val chatRoomJpaRepository: ChatRoomJpaRepository
) {

    /**
     * 모든 활성 질문 조회
     */
    fun findActiveQuestions(): List<Question> {
        return questionJpaRepository.findActiveQuestions()
    }
    
    /**
     * 회원가입용 활성 질문 조회 (IF, BALANCE_ONE 카테고리 제외)
     */
    fun findActiveQuestionsForSignup(): List<Question> {
        return questionJpaRepository.findActiveQuestionsForSignup()
    }
    
    /**
     * ID로 질문 조회
     */
    fun findQuestionById(questionId: Long): Question {
        return questionJpaRepository.findById(questionId).orElseThrow {
            IllegalArgumentException("질문을 찾을 수 없습니다. ID: $questionId")
        }
    }
    
    /**
     * 채팅방에서 사용하지 않은 질문들 조회
     */
    fun findUnusedQuestionsByChatRoom(chatRoomId: Long): List<Question> {
        return questionJpaRepository.findUnusedQuestionsByChatRoom(chatRoomId)
    }

    /**
     * 질문 리스트에서 랜덤 선택
     */
    fun selectRandomQuestion(questions: List<Question>): Question {
        if (questions.isEmpty()) {
            throw IllegalArgumentException("선택할 수 있는 질문이 없습니다.")
        }
        return questions.random()
    }
    
    /**
     * 질문을 사용된 것으로 표시
     */
    @Transactional
    fun markQuestionAsUsed(chatRoomId: Long, question: Question, requestedBy: Member) {
        val chatRoom = chatRoomJpaRepository.findById(chatRoomId).orElseThrow {
            IllegalArgumentException("채팅방을 찾을 수 없습니다.")
        }
        
        val chatRoomQuestion = ChatRoomQuestion.create(chatRoom, question, requestedBy)
        chatRoomQuestionJpaRepository.save(chatRoomQuestion)
    }

    // ========== 관리자용 메서드들 ==========
    
    /**
     * 필터 조건으로 질문 목록 조회
     */
    fun findQuestionsWithFilter(
        keyword: String?,
        category: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<Question> {
        val categoryEnum = if (category.isNullOrBlank()) null else QuestionCategory.valueOf(category)
        return questionJpaRepository.findAllWithFilter(keyword, categoryEnum, isActive, pageable)
    }
    
    /**
     * 새 질문 생성
     */
    @Transactional
    fun createQuestion(
        content: String,
        category: QuestionCategory,
        description: String?,
        isActive: Boolean
    ): Question {
        val question = Question(
            content = content,
            category = category,
            description = description,
            isActive = isActive
        )
        return questionJpaRepository.save(question)
    }
    
    /**
     * 질문 수정
     */
    @Transactional
    fun updateQuestion(
        questionId: Long,
        content: String,
        category: QuestionCategory,
        description: String?,
        isActive: Boolean
    ): Question {
        val question = findQuestionById(questionId)
        
        question.updateContent(content)
        question.updateCategory(category)
        question.updateDescription(description)
        question.updateIsActive(isActive)
        
        return questionJpaRepository.save(question)
    }
    
    /**
     * 질문 삭제
     */
    @Transactional
    fun deleteQuestion(questionId: Long) {
        if (!questionJpaRepository.existsById(questionId)) {
            throw IllegalArgumentException("질문을 찾을 수 없습니다. ID: $questionId")
        }
        questionJpaRepository.deleteById(questionId)
    }
    
    /**
     * 질문 상태 토글
     */
    @Transactional
    fun toggleQuestionStatus(questionId: Long): Question {
        val question = findQuestionById(questionId)
        question.toggleActive()
        return questionJpaRepository.save(question)
    }
}
