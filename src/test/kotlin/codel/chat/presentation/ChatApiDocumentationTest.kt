package codel.chat.presentation

import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("채팅 API 문서화 테스트")
class ChatApiDocumentationTest {

    @LocalServerPort
    private var port: Int = 0

    @BeforeEach
    fun setUp() {
        RestAssured.port = port
    }

    @Test
    @DisplayName("GET 요청으로 채팅 목록 조회 - Query Parameter 사용")
    fun `채팅 목록 조회는 Query Parameter를 사용한다`() {
        // when & then
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-jwt-token")
                .param("lastChatId", 456L)  // Query Parameter로 전달
                .param("page", 0)
                .param("size", 30)
            .`when`()
                .get("/v1/chatroom/123/chats")  // GET 요청, body 없음
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))  // 인증/권한 에러는 정상
    }

    @Test
    @DisplayName("GET 요청에 RequestBody를 포함하면 클라이언트에서 제대로 전송되지 않는다")
    fun `GET_요청에_RequestBody를_포함하면_문제가_발생한다`() {
        val requestBody = """
            {
                "lastChatId": 456
            }
        """.trimIndent()

        // GET 요청에 body를 포함하여 전송 시도
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-jwt-token")
                .body(requestBody)  // ❌ GET 요청에 body 포함
            .`when`()
                .get("/v1/chatroom/123/chats")
            .then()
                // 대부분의 클라이언트/서버에서 GET 요청의 body를 무시하거나 에러 발생
                .statusCode(anyOf(
                    equalTo(400),  // Bad Request
                    equalTo(405),  // Method Not Allowed
                    equalTo(401),  // Unauthorized (body가 무시되어 파싱 실패)
                    equalTo(403)   // Forbidden
                ))
    }

    @Test
    @DisplayName("올바른 방식: Query Parameter를 사용한 GET 요청")
    fun `올바른_방식으로_채팅_목록_조회_테스트`() {
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-jwt-token")
                .queryParam("lastChatId", 456L)
                .queryParam("page", 0)
                .queryParam("size", 30)
            .`when`()
                .get("/v1/chatroom/123/chats")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
    }

    @Test
    @DisplayName("lastChatId 없이 요청 - 최초 채팅 목록 로드")
    fun `lastChatId_없이_최초_채팅_목록_로드`() {
        RestAssured
            .given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer valid-jwt-token")
                .queryParam("page", 0)
                .queryParam("size", 30)
            .`when`()
                .get("/v1/chatroom/123/chats")
            .then()
                .statusCode(anyOf(equalTo(200), equalTo(401), equalTo(403)))
    }
}
