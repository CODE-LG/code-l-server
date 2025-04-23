package codel.member.domain

import codel.member.exception.MemberException
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class FaceImageTest {
    @DisplayName("페이스 이미지는 3장이여야 한다.")
    @Test
    fun initTest() {
        assertAll(
            {
                assertDoesNotThrow {
                    FaceImage(listOf("url1", "url2", "url3"))
                }
            },
            {
                assertThrows(MemberException::class.java) {
                    FaceImage(listOf("url1", "url2"))
                }
            },
            {
                assertThrows(MemberException::class.java) {
                    FaceImage(listOf("url1", "url2", "url3", "url4"))
                }
            },
        )
    }
}
