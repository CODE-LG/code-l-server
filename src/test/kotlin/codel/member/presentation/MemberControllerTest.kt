package codel.member.presentation

import codel.config.TestFixture
import codel.member.domain.MemberStatus
import codel.member.presentation.response.MemberLoginResponse
import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberControllerTest : TestFixture() {
    @LocalServerPort
    var port: Int = 0

    @BeforeEach
    fun setup() {
        RestAssured.port = port
    }

    @DisplayName("Member 중복 로그인 테스트")
    @Test
    fun saveMember() {
        val request =
            mapOf(
                "oauthType" to "APPLE",
                "oauthId" to "hogee",
            )
        val expectedResponse = MemberLoginResponse(MemberStatus.SIGNUP)

        val token =
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .`when`()
                .post("/v1/member/login")
                .then()
                .statusCode(200)
                .extract()
                .header("Authorization")
                .removePrefix("Bearer ")

        val response =
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer $token")
                .body(request)
                .`when`()
                .post("/v1/member/login")
                .then()
                .statusCode(200)
                .extract()
                .`as`(MemberLoginResponse::class.java)

        Assertions.assertThat(expectedResponse.memberStatus).isEqualTo(response.memberStatus)
    }

    @DisplayName("허용된 URL은 인증 없이 접근 가능해야 한다")
    @Test
    fun excludePathTest() {
        val request =
            mapOf(
                "oauthType" to "APPLE",
                "oauthId" to "hogee",
            )

        given()
            .contentType(ContentType.JSON)
            .body(request)
            .`when`()
            .post("/v1/member/login")
            .then()
            .statusCode(200)
    }

    @DisplayName("토큰 없으면 401을 응답해야 한다")
    @Test
    fun unAuthorizeUserTest() {
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/v1/user")
            .then()
            .statusCode(401)
    }

    @DisplayName("로그인 성공 시 accessToken을 발급한다")
    @Test
    fun issueTokenOnLoginSuccess() {
        val request =
            mapOf(
                "oauthType" to "APPLE",
                "oauthId" to "hogee",
            )

        val response =
            given()
                .contentType(ContentType.JSON)
                .body(request)
                .`when`()
                .post("/v1/member/login")
                .then()
                .statusCode(200)
                .extract()

        val authHeader = response.header("Authorization")
        assertTrue(authHeader != null && authHeader.startsWith("Bearer "))
    }

    @DisplayName("프로필을 등록 테스트")
    @Test
    fun saveProfile() {
        val request =
            mapOf(
                "codeName" to "seok",
                "age" to 28,
                "job" to "백엔드 개발자",
                "alcohol" to "자주 마심",
                "smoke" to "비흡연자 - 흡연자와 교류 NO",
                "hobby" to listOf("영화 & 드라마", "여행 & 캠핑"),
                "style" to listOf("표현을 잘하는 직진형", "상대가 필요할 때 항상 먼저 연락하는 스타일"),
                "bigCity" to "경기도",
                "smallCity" to "성남시",
                "mbti" to "isfj",
                "introduce" to "잘부탁드립니다!",
            )

        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer $token")
            .body(request)
            .`when`()
            .post("/v1/member/profile")
            .then()
            .statusCode(200)
            .extract()
    }
}
