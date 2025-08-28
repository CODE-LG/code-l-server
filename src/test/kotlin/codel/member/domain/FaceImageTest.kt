package codel.member.domain

import codel.member.exception.MemberException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class FaceImageTest {

    @DisplayName("정확히 2개의 이미지 URL로 FaceImage를 생성할 수 있다")
    @Test
    fun createFaceImage_valid() {
        // given
        val urls = listOf("url1", "url2")

        // when
        val faceImage = FaceImage(urls)

        // then
        assertThat(faceImage.urls).containsExactly("url1", "url2")
    }

    @DisplayName("이미지 URL이 3개가 아니면 예외가 발생한다 (2개)")
    @Test
    fun createFaceImage_tooFew() {
        // given
        val urls = listOf("url1")

        // when & then
        val exception = assertThrows(MemberException::class.java) {
            FaceImage(urls)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("얼굴 이미지 URL은 정확히 2개여야 합니다.")
    }

    @DisplayName("이미지 URL이 3개가 아니면 예외가 발생한다 (4개)")
    @Test
    fun createFaceImage_tooMany() {
        // given
        val urls = listOf("url1", "url2", "url3")

        // when & then
        val exception = assertThrows(MemberException::class.java) {
            FaceImage(urls)
        }
        assertThat(exception.status).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(exception.message).contains("얼굴 이미지 URL은 정확히 2개여야 합니다.")
    }

    @DisplayName("serializeAttribute는 URL을 ,로 연결한다")
    @Test
    fun serializeAttribute() {
        // given
        val urls = listOf("a", "b")
        val faceImage = FaceImage(urls)

        // when
        val serialized = faceImage.serializeAttribute()

        // then
        assertThat(serialized).isEqualTo("a,b")
    }
} 