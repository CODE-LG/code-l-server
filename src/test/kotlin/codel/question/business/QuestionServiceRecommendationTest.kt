package codel.question.business

import codel.chat.infrastructure.ChatRoomJpaRepository
import codel.chat.infrastructure.ChatRoomQuestionJpaRepository
import codel.question.domain.GroupPolicy
import codel.question.domain.Question
import codel.question.domain.QuestionCategory
import codel.question.domain.QuestionGroup
import codel.question.infrastructure.QuestionJpaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

class QuestionServiceRecommendationTest {

    private lateinit var questionJpaRepository: QuestionJpaRepository
    private lateinit var chatRoomQuestionJpaRepository: ChatRoomQuestionJpaRepository
    private lateinit var chatRoomJpaRepository: ChatRoomJpaRepository
    private lateinit var questionService: QuestionService

    @BeforeEach
    fun setUp() {
        questionJpaRepository = mock(QuestionJpaRepository::class.java)
        chatRoomQuestionJpaRepository = mock(ChatRoomQuestionJpaRepository::class.java)
        chatRoomJpaRepository = mock(ChatRoomJpaRepository::class.java)
        questionService = QuestionService(
            questionJpaRepository,
            chatRoomQuestionJpaRepository,
            chatRoomJpaRepository
        )
    }

    private fun createQuestion(id: Long, category: QuestionCategory, group: QuestionGroup): Question {
        return Question(
            id = id,
            content = "Test question $id",
            category = category,
            questionGroup = group,
            isActive = true,
            description = null
        )
    }

    @Nested
    @DisplayName("A_THEN_B 그룹 정책 테스트")
    inner class AThenBPolicyTest {

        @DisplayName("A그룹 질문이 있으면 A그룹에서 추천한다")
        @Test
        fun recommend_from_group_a_when_available() {
            // given
            val chatRoomId = 1L
            val category = QuestionCategory.VALUES_CODE
            val groupAQuestions = listOf(
                createQuestion(1L, category, QuestionGroup.A),
                createQuestion(2L, category, QuestionGroup.A)
            )

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.A
            )).thenReturn(groupAQuestions)

            // when
            val result = questionService.recommendQuestionForChat(chatRoomId, category)

            // then
            assertTrue(result is QuestionRecommendationResult.Success)
            val successResult = result as QuestionRecommendationResult.Success
            assertEquals(QuestionGroup.A, successResult.question.questionGroup)

            // B그룹 조회가 호출되지 않아야 함
            verify(questionJpaRepository, never()).findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.B
            )
        }

        @DisplayName("A그룹이 소진되면 B그룹에서 추천한다")
        @Test
        fun recommend_from_group_b_when_group_a_exhausted() {
            // given
            val chatRoomId = 1L
            val category = QuestionCategory.VALUES_CODE
            val groupBQuestions = listOf(
                createQuestion(3L, category, QuestionGroup.B),
                createQuestion(4L, category, QuestionGroup.B)
            )

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.A
            )).thenReturn(emptyList())

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.B
            )).thenReturn(groupBQuestions)

            // when
            val result = questionService.recommendQuestionForChat(chatRoomId, category)

            // then
            assertTrue(result is QuestionRecommendationResult.Success)
            val successResult = result as QuestionRecommendationResult.Success
            assertEquals(QuestionGroup.B, successResult.question.questionGroup)
        }

        @DisplayName("A그룹과 B그룹 모두 소진되면 Exhausted를 반환한다")
        @Test
        fun return_exhausted_when_all_groups_exhausted() {
            // given
            val chatRoomId = 1L
            val category = QuestionCategory.VALUES_CODE

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.A
            )).thenReturn(emptyList())

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategoryAndGroup(
                chatRoomId, category, QuestionGroup.B
            )).thenReturn(emptyList())

            // when
            val result = questionService.recommendQuestionForChat(chatRoomId, category)

            // then
            assertTrue(result is QuestionRecommendationResult.Exhausted)
        }
    }

    @Nested
    @DisplayName("RANDOM 그룹 정책 테스트")
    inner class RandomPolicyTest {

        @DisplayName("TENSION_UP은 그룹 구분 없이 랜덤 추천한다")
        @Test
        fun tension_up_recommend_randomly() {
            // given
            val chatRoomId = 1L
            val category = QuestionCategory.TENSION_UP
            val allQuestions = listOf(
                createQuestion(1L, category, QuestionGroup.RANDOM),
                createQuestion(2L, category, QuestionGroup.RANDOM)
            )

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategory(
                chatRoomId, category
            )).thenReturn(allQuestions)

            // when
            val result = questionService.recommendQuestionForChat(chatRoomId, category)

            // then
            assertTrue(result is QuestionRecommendationResult.Success)

            // findUnusedQuestionsByChatRoomAndCategory가 호출되었는지 확인
            verify(questionJpaRepository).findUnusedQuestionsByChatRoomAndCategory(chatRoomId, category)
        }

        @DisplayName("TENSION_UP 질문이 소진되면 Exhausted를 반환한다")
        @Test
        fun tension_up_return_exhausted_when_empty() {
            // given
            val chatRoomId = 1L
            val category = QuestionCategory.TENSION_UP

            `when`(questionJpaRepository.findUnusedQuestionsByChatRoomAndCategory(
                chatRoomId, category
            )).thenReturn(emptyList())

            // when
            val result = questionService.recommendQuestionForChat(chatRoomId, category)

            // then
            assertTrue(result is QuestionRecommendationResult.Exhausted)
        }
    }

    @Nested
    @DisplayName("카테고리별 그룹 정책 확인")
    inner class CategoryGroupPolicyTest {

        @DisplayName("IF 카테고리는 A_THEN_B 정책을 사용한다")
        @Test
        fun if_category_uses_a_then_b() {
            assertEquals(GroupPolicy.A_THEN_B, QuestionCategory.IF.chatGroupPolicy)
        }

        @DisplayName("SECRET 카테고리는 A_THEN_B 정책을 사용한다")
        @Test
        fun secret_category_uses_a_then_b() {
            assertEquals(GroupPolicy.A_THEN_B, QuestionCategory.SECRET.chatGroupPolicy)
        }

        @DisplayName("TENSION_UP 카테고리는 RANDOM 정책을 사용한다")
        @Test
        fun tension_up_category_uses_random() {
            assertEquals(GroupPolicy.RANDOM, QuestionCategory.TENSION_UP.chatGroupPolicy)
        }
    }
}
