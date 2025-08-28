package codel.question.business

import codel.question.infrastructure.QuestionJpaRepository
import codel.question.domain.Question
import codel.chat.domain.ChatRoomQuestion
import codel.chat.infrastructure.ChatRoomQuestionJpaRepository
import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.member.domain.Member
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
}
