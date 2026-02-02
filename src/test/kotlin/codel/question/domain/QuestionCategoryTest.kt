package codel.question.domain

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class QuestionCategoryTest {

    @Nested
    @DisplayName("카테고리 용도 테스트")
    inner class CategoryUsageTest {

        @DisplayName("VALUES는 회원가입에서만 사용된다")
        @Test
        fun values_used_in_signup_only() {
            // given
            val category = QuestionCategory.VALUES

            // then
            assertTrue(category.usedInSignup)
            assertFalse(category.usedInChat)
            assertTrue(category.isSignupCategory())
            assertFalse(category.isChatCategory())
            assertEquals(GroupPolicy.NONE, category.chatGroupPolicy)
        }

        @DisplayName("VALUES_CODE는 채팅방에서만 사용된다")
        @Test
        fun values_code_used_in_chat_only() {
            // given
            val category = QuestionCategory.VALUES_CODE

            // then
            assertFalse(category.usedInSignup)
            assertTrue(category.usedInChat)
            assertFalse(category.isSignupCategory())
            assertTrue(category.isChatCategory())
            assertEquals(GroupPolicy.A_THEN_B, category.chatGroupPolicy)
        }

        @DisplayName("FAVORITE은 회원가입에서만 사용된다")
        @Test
        fun favorite_used_in_signup_only() {
            // given
            val category = QuestionCategory.FAVORITE

            // then
            assertTrue(category.usedInSignup)
            assertFalse(category.usedInChat)
            assertTrue(category.isSignupCategory())
            assertFalse(category.isChatCategory())
        }

        @DisplayName("TENSION_UP은 채팅방에서만 사용된다")
        @Test
        fun tension_up_used_in_chat_only() {
            // given
            val category = QuestionCategory.TENSION_UP

            // then
            assertFalse(category.usedInSignup)
            assertTrue(category.usedInChat)
            assertFalse(category.isSignupCategory())
            assertTrue(category.isChatCategory())
        }

        @DisplayName("IF는 채팅방에서만 사용된다")
        @Test
        fun if_used_in_chat_only() {
            // given
            val category = QuestionCategory.IF

            // then
            assertFalse(category.usedInSignup)
            assertTrue(category.usedInChat)
        }

        @DisplayName("SECRET은 채팅방에서만 사용된다")
        @Test
        fun secret_used_in_chat_only() {
            // given
            val category = QuestionCategory.SECRET

            // then
            assertFalse(category.usedInSignup)
            assertTrue(category.usedInChat)
        }
    }

    @Nested
    @DisplayName("그룹 정책 테스트")
    inner class GroupPolicyTest {

        @DisplayName("VALUES_CODE는 A_THEN_B 그룹 정책을 사용한다")
        @Test
        fun values_code_has_a_then_b_policy() {
            // given
            val category = QuestionCategory.VALUES_CODE

            // then
            assertEquals(GroupPolicy.A_THEN_B, category.chatGroupPolicy)
        }

        @DisplayName("TENSION_UP은 RANDOM 그룹 정책을 사용한다")
        @Test
        fun tension_up_has_random_policy() {
            // given
            val category = QuestionCategory.TENSION_UP

            // then
            assertEquals(GroupPolicy.RANDOM, category.chatGroupPolicy)
        }

        @DisplayName("IF는 A_THEN_B 그룹 정책을 사용한다")
        @Test
        fun if_has_a_then_b_policy() {
            // given
            val category = QuestionCategory.IF

            // then
            assertEquals(GroupPolicy.A_THEN_B, category.chatGroupPolicy)
        }

        @DisplayName("SECRET은 A_THEN_B 그룹 정책을 사용한다")
        @Test
        fun secret_has_a_then_b_policy() {
            // given
            val category = QuestionCategory.SECRET

            // then
            assertEquals(GroupPolicy.A_THEN_B, category.chatGroupPolicy)
        }

        @DisplayName("회원가입 전용 카테고리는 NONE 그룹 정책을 사용한다")
        @Test
        fun signup_only_categories_have_none_policy() {
            // given
            val signupOnlyCategories = listOf(
                QuestionCategory.VALUES,
                QuestionCategory.FAVORITE,
                QuestionCategory.CURRENT_ME,
                QuestionCategory.DATE,
                QuestionCategory.MEMORY,
                QuestionCategory.WANT_TALK
            )

            // then
            signupOnlyCategories.forEach { category ->
                assertEquals(GroupPolicy.NONE, category.chatGroupPolicy,
                    "${category.name}는 NONE 정책이어야 합니다")
            }
        }
    }

    @Nested
    @DisplayName("카테고리 조회 테스트")
    inner class CategoryQueryTest {

        @DisplayName("회원가입 카테고리 목록을 조회한다")
        @Test
        fun getSignupCategories() {
            // when
            val signupCategories = QuestionCategory.getSignupCategories()

            // then
            assertTrue(signupCategories.contains(QuestionCategory.VALUES))
            assertTrue(signupCategories.contains(QuestionCategory.FAVORITE))
            assertTrue(signupCategories.contains(QuestionCategory.CURRENT_ME))
            assertTrue(signupCategories.contains(QuestionCategory.DATE))
            assertTrue(signupCategories.contains(QuestionCategory.MEMORY))
            assertTrue(signupCategories.contains(QuestionCategory.WANT_TALK))
            assertFalse(signupCategories.contains(QuestionCategory.VALUES_CODE))
            assertFalse(signupCategories.contains(QuestionCategory.TENSION_UP))
            assertFalse(signupCategories.contains(QuestionCategory.IF))
            assertFalse(signupCategories.contains(QuestionCategory.SECRET))
        }

        @DisplayName("채팅방 카테고리 목록을 조회한다")
        @Test
        fun getChatCategories() {
            // when
            val chatCategories = QuestionCategory.getChatCategories()

            // then
            assertTrue(chatCategories.contains(QuestionCategory.VALUES_CODE))
            assertTrue(chatCategories.contains(QuestionCategory.TENSION_UP))
            assertTrue(chatCategories.contains(QuestionCategory.IF))
            assertTrue(chatCategories.contains(QuestionCategory.SECRET))
            assertFalse(chatCategories.contains(QuestionCategory.VALUES))
            assertFalse(chatCategories.contains(QuestionCategory.FAVORITE))
            assertFalse(chatCategories.contains(QuestionCategory.DATE))
        }

        @DisplayName("문자열로 카테고리를 조회한다")
        @Test
        fun fromString_valid() {
            // when & then
            assertEquals(QuestionCategory.VALUES, QuestionCategory.fromString("VALUES"))
            assertEquals(QuestionCategory.VALUES, QuestionCategory.fromString("values"))
            assertEquals(QuestionCategory.TENSION_UP, QuestionCategory.fromString("TENSION_UP"))
        }

        @DisplayName("잘못된 문자열로 조회 시 null을 반환한다")
        @Test
        fun fromString_invalid() {
            // when & then
            assertNull(QuestionCategory.fromString("INVALID"))
            assertNull(QuestionCategory.fromString(null))
            assertNull(QuestionCategory.fromString(""))
        }
    }
}
