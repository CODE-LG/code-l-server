package codel.question.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class QuestionGroupTest {

    @DisplayName("QuestionGroup enum 값들이 올바른 displayName을 가진다")
    @Test
    fun questionGroup_displayName() {
        // then
        assertEquals("A그룹", QuestionGroup.A.displayName)
        assertEquals("B그룹", QuestionGroup.B.displayName)
        assertEquals("랜덤", QuestionGroup.RANDOM.displayName)
    }

    @DisplayName("QuestionGroup enum에 3개의 값이 존재한다")
    @Test
    fun questionGroup_values_count() {
        // then
        assertEquals(3, QuestionGroup.entries.size)
    }
}
