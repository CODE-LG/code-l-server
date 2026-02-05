package codel.question.business

import codel.question.infrastructure.QuestionJpaRepository
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import codel.question.domain.QuestionGroup
import codel.question.domain.GroupPolicy
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

    // ========== 채팅방 질문 추천 (카테고리 기반) ==========

    /**
     * 채팅방 질문 추천 (카테고리별 그룹 정책 적용)
     *
     * @param chatRoomId 채팅방 ID
     * @param category 선택한 카테고리
     * @return 추천 결과 (Success 또는 Exhausted)
     */
    fun recommendQuestionForChat(
        chatRoomId: Long,
        category: QuestionCategory
    ): QuestionRecommendationResult {
        if (!category.isChatCategory()) {
            throw IllegalArgumentException("채팅방에서 사용할 수 없는 카테고리입니다: ${category.displayName}")
        }

        return when (category.chatGroupPolicy) {
            GroupPolicy.RANDOM -> recommendRandom(chatRoomId, category)
            GroupPolicy.A_THEN_B -> recommendWithGroupPriority(chatRoomId, category)
            GroupPolicy.NONE -> throw IllegalStateException("채팅방용 카테고리에 NONE 정책은 허용되지 않습니다.")
        }
    }

    /**
     * A그룹 우선 → B그룹 순서로 추천
     */
    private fun recommendWithGroupPriority(
        chatRoomId: Long,
        category: QuestionCategory
    ): QuestionRecommendationResult {
        // 1. A그룹에서 미사용 질문 조회
        val groupAQuestions = questionJpaRepository
            .findUnusedQuestionsByChatRoomAndCategoryAndGroup(chatRoomId, category, QuestionGroup.A)

        if (groupAQuestions.isNotEmpty()) {
            return QuestionRecommendationResult.Success(groupAQuestions.random())
        }

        // 2. A그룹 소진 시 B그룹에서 조회
        val groupBQuestions = questionJpaRepository
            .findUnusedQuestionsByChatRoomAndCategoryAndGroup(chatRoomId, category, QuestionGroup.B)

        if (groupBQuestions.isNotEmpty()) {
            return QuestionRecommendationResult.Success(groupBQuestions.random())
        }

        // 3. 모두 소진
        return QuestionRecommendationResult.Exhausted
    }

    /**
     * 그룹 구분 없이 랜덤 추천
     */
    private fun recommendRandom(
        chatRoomId: Long,
        category: QuestionCategory
    ): QuestionRecommendationResult {
        val questions = questionJpaRepository
            .findUnusedQuestionsByChatRoomAndCategory(chatRoomId, category)

        return if (questions.isNotEmpty()) {
            QuestionRecommendationResult.Success(questions.random())
        } else {
            QuestionRecommendationResult.Exhausted
        }
    }
    
    /**
     * 질문을 사용된 것으로 표시
     *
     * @param isInitial true면 초기 질문(KPI 제외), false면 질문하기 버튼 클릭(KPI 집계 대상)
     */
    @Transactional
    fun markQuestionAsUsed(
        chatRoomId: Long,
        question: Question,
        requestedBy: Member,
        isInitial: Boolean = false
    ) {
        val chatRoom = chatRoomJpaRepository.findById(chatRoomId).orElseThrow {
            IllegalArgumentException("채팅방을 찾을 수 없습니다.")
        }

        val chatRoomQuestion = if (isInitial) {
            ChatRoomQuestion.createInitial(chatRoom, question, requestedBy)
        } else {
            ChatRoomQuestion.create(chatRoom, question, requestedBy)
        }

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
     * 필터 조건으로 질문 목록 조회 (그룹 포함)
     */
    fun findQuestionsWithFilterV2(
        keyword: String?,
        category: String?,
        questionGroup: String?,
        isActive: Boolean?,
        pageable: Pageable
    ): Page<Question> {
        val categoryEnum = if (category.isNullOrBlank()) null else QuestionCategory.valueOf(category)
        val groupEnum = if (questionGroup.isNullOrBlank()) null else QuestionGroup.valueOf(questionGroup)
        return questionJpaRepository.findAllWithFilterV2(keyword, categoryEnum, groupEnum, isActive, pageable)
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
     * 새 질문 생성 (그룹 포함)
     */
    @Transactional
    fun createQuestionV2(
        content: String,
        category: QuestionCategory,
        questionGroup: QuestionGroup,
        description: String?,
        isActive: Boolean
    ): Question {
        val question = Question(
            content = content,
            category = category,
            questionGroup = questionGroup,
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
     * 질문 수정 (그룹 포함)
     */
    @Transactional
    fun updateQuestionV2(
        questionId: Long,
        content: String,
        category: QuestionCategory,
        questionGroup: QuestionGroup,
        description: String?,
        isActive: Boolean
    ): Question {
        val question = findQuestionById(questionId)

        question.updateContent(content)
        question.updateCategory(category)
        question.updateQuestionGroup(questionGroup)
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
