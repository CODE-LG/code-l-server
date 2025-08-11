package codel.question.business

import codel.question.domain.Question
import codel.question.infrastructure.QuestionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class QuestionDomainService(
    private val questionJpaRepository: QuestionJpaRepository
) {
    
    fun findActiveQuestions(): List<Question> {
        return questionJpaRepository.findActiveQuestions()
    }
    
    fun findQuestionsByCategory(category: String): List<Question> {
        return questionJpaRepository.findActiveQuestionsByCategory(category)
    }
    
    fun findUnusedQuestionsByChatRoom(chatRoomId: Long): List<Question> {
        return questionJpaRepository.findUnusedQuestionsByChatRoom(chatRoomId)
    }
    
    fun findQuestionById(questionId: Long): Question? {
        return questionJpaRepository.findById(questionId).orElse(null)
    }
}
