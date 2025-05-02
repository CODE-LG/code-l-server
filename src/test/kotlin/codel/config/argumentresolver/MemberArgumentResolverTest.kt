package codel.config.argumentresolver

import codel.member.business.MemberService
import codel.member.domain.Member
import codel.member.domain.MemberStatus
import codel.member.domain.OauthType
import jakarta.servlet.http.HttpServletRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.core.MethodParameter
import org.springframework.web.context.request.NativeWebRequest

class MemberArgumentResolverTest {
    private lateinit var memberService: MemberService
    private lateinit var resolver: MemberArgumentResolver

    @BeforeEach
    fun setUp() {
        memberService = mock(MemberService::class.java)
        resolver = MemberArgumentResolver(memberService)
    }

    @DisplayName("ArgumentResolver는 Member 정보를 반환한다.")
    @Test
    fun resolveArgumentTest() {
        val memberId = 1L
        val oauthId = "seok"
        val oauthType = OauthType.KAKAO

        val fakeMember =
            Member(
                id = memberId,
                oauthId = oauthId,
                oauthType = oauthType,
                memberStatus = MemberStatus.SIGNUP,
                email = "hogee@hogee",
            )

        val httpRequest = mock(HttpServletRequest::class.java)
        `when`(httpRequest.getAttribute("memberId")).thenReturn(memberId.toString())

        val webRequest = mock(NativeWebRequest::class.java)
        `when`(webRequest.getNativeRequest(HttpServletRequest::class.java)).thenReturn(httpRequest)

        `when`(memberService.findMember(memberId)).thenReturn(fakeMember)

        val result =
            resolver.resolveArgument(
                mock(MethodParameter::class.java),
                null,
                webRequest,
                null,
            )

        assertEquals(fakeMember, result)
    }
}
