package codel.member.domain

import codel.member.exception.MemberException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class CodeImageVOTest {

    @DisplayName("1~3개의 이미지 URL로 CodeImage를 생성할 수 있다")
    @Test
    fun createCodeImage_valid() {
        // given
        val urls = listOf("url1", "url2", "url3")

        // when
        val codeImage = CodeImageVO(urls)

        // then
        assertThat(codeImage.urls).containsExactly("url1", "url2", "url3")
    }

    @DisplayName("이미지 URL이 0개면 예외가 발생한다")
    @Test
    fun createCodeImage_empty() {
        // given
        val urls = emptyList<String>()

        // when & then
        val exception = assertThrows(MemberException::class.java) {
            CodeImageVO(urls)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("코드 이미지 URL은 1개 이상 3개 이하이어야 합니다.")
    }

    @DisplayName("이미지 URL이 4개 이상이면 예외가 발생한다")
    @Test
    fun createCodeImage_tooMany() {
        // given
        val urls = listOf("url1", "url2", "url3", "url4")

        // when & then
        val exception = assertThrows(MemberException::class.java) {
            CodeImageVO(urls)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("코드 이미지 URL은 1개 이상 3개 이하이어야 합니다.")
    }

    @DisplayName("serializeAttribute는 URL을 ,로 연결한다")
    @Test
    fun serializeAttribute() {
        // given
        val urls = listOf("a", "b", "c")
        val codeImage = CodeImageVO(urls)

        // when
        val serialized = codeImage.serializeAttribute()

        // then
        assertThat(serialized).isEqualTo("a,b,c")
    }
}
